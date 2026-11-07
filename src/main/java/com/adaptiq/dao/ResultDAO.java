
package com.adaptiq.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.AttemptStatus;
import com.adaptiq.model.ExamAttempt;
import com.adaptiq.service.EvaluationResult;

/**
 * Transactional answer persistence and server-side automatic evaluation.
 */
public final class ResultDAO implements ResultRepository {

    private final ConnectionProvider connections;

    public ResultDAO() {
        this(DatabaseConnection::getConnection);
    }

    public ResultDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    @Override
    public ExamAttempt startOrResume(long studentId, long examId) {
        try (Connection c = connections.getConnection()) {
            c.setAutoCommit(false);

            try {
                ExamAttempt active = findActive(c, studentId, examId);

                if (active != null) {
                    c.commit();
                    return active;
                }

                try (PreparedStatement s = c.prepareStatement(
                        "INSERT INTO exam_attempts (student_id,exam_id,status) " +
                        "SELECT ?,exam_id,'IN_PROGRESS' FROM exams " +
                        "WHERE exam_id=? AND exam_mode='NORMAL' AND status='PUBLISHED'",
                        Statement.RETURN_GENERATED_KEYS)) {

                    s.setLong(1, studentId);
                    s.setLong(2, examId);

                    if (s.executeUpdate() != 1) {
                        throw new DatabaseException("This examination is not available.");
                    }

                    try (ResultSet k = s.getGeneratedKeys()) {
                        if (!k.next()) {
                            throw new DatabaseException(
                                    "Attempt creation did not return an identifier.");
                        }

                        long id = k.getLong(1);
                        ExamAttempt attempt = findAttempt(c, id, studentId);

                        c.commit();
                        return attempt;
                    }
                }

            } catch (SQLException | RuntimeException e) {
                rollback(c);
                throw wrap("Could not start the examination.", e);

            } finally {
                auto(c);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not start the examination.", e);
        }
    }

    @Override
    public Optional<ExamAttempt> findAttempt(long id, long student) {
        try (Connection c = connections.getConnection()) {
            return Optional.ofNullable(findAttempt(c, id, student));
        } catch (SQLException e) {
            throw new DatabaseException("Could not retrieve examination attempt.", e);
        }
    }

    @Override
    public void saveAnswer(long attempt, long student, long question, Long option) {

        String sql = """
                INSERT INTO student_answers
                (attempt_id, question_id, selected_option_id, is_correct,
                 marks_obtained, answered_at)

                SELECT a.attempt_id, eq.question_id, ?, NULL, 0, CURRENT_TIMESTAMP

                FROM exam_attempts a
                JOIN exam_questions eq ON eq.exam_id = a.exam_id
                LEFT JOIN question_options o
                    ON o.option_id = ? AND o.question_id = eq.question_id

                WHERE a.attempt_id = ?
                AND a.student_id = ?
                AND a.status = 'IN_PROGRESS'
                AND eq.question_id = ?
                AND (? IS NULL OR o.option_id IS NOT NULL)

                ON DUPLICATE KEY UPDATE
                    selected_option_id = VALUES(selected_option_id),
                    is_correct = NULL,
                    marks_obtained = 0,
                    answered_at = CURRENT_TIMESTAMP
                """;

        try (Connection c = connections.getConnection();
             PreparedStatement s = c.prepareStatement(sql)) {

            if (option == null) {
                s.setNull(1, Types.BIGINT);
                s.setNull(2, Types.BIGINT);
                s.setNull(6, Types.BIGINT);
            } else {
                s.setLong(1, option);
                s.setLong(2, option);
                s.setLong(6, option);
            }

            s.setLong(3, attempt);
            s.setLong(4, student);
            s.setLong(5, question);

            int affectedRows = s.executeUpdate();

            if (affectedRows == 0) {
                throw new DatabaseException(
                        "The answer could not be saved for this examination.");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not save the answer.", e);
        }
    }

    @Override
    public EvaluationResult submit(long attempt, long student, boolean expired) {

        try (Connection c = connections.getConnection()) {
            c.setAutoCommit(false);

            try {
                ExamAttempt a = findAttemptForUpdate(c, attempt, student);

                if (a == null || a.getStatus() != AttemptStatus.IN_PROGRESS) {
                    throw new DatabaseException(
                            "This attempt has already been submitted or is unavailable.");
                }

                List<Row> rows = rows(c, attempt);

                if (rows.isEmpty()) {
                    throw new DatabaseException("The examination has no questions.");
                }

                BigDecimal total = BigDecimal.ZERO;
                BigDecimal score = BigDecimal.ZERO;

                long correct = 0;
                long attempted = 0;

                for (Row row : rows) {
                    total = total.add(row.marks);

                    if (row.optionId != null) {
                        attempted++;

                        if (row.correct) {
                            correct++;
                            score = score.add(row.marks);
                        }

                        updateAnswerEvaluation(c, attempt, row);
                    }
                }

                long unanswered = rows.size() - attempted;
                long incorrect = attempted - correct;

                BigDecimal accuracy = attempted == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(correct)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(
                                        BigDecimal.valueOf(attempted),
                                        2,
                                        RoundingMode.HALF_UP
                                );

                BigDecimal percentage = total.signum() == 0
                        ? BigDecimal.ZERO
                        : score.multiply(BigDecimal.valueOf(100))
                                .divide(total, 2, RoundingMode.HALF_UP);

                try (PreparedStatement s = c.prepareStatement(
                        "UPDATE exam_attempts SET submitted_at=CURRENT_TIMESTAMP," +
                        "score=?,percentage=?,accuracy=?,status=? WHERE attempt_id=?")) {

                    s.setBigDecimal(1, score);
                    s.setBigDecimal(2, percentage);
                    s.setBigDecimal(3, accuracy);
                    s.setString(4, expired ? "EXPIRED" : "SUBMITTED");
                    s.setLong(5, attempt);

                    if (s.executeUpdate() != 1) {
                        throw new DatabaseException(
                                "Could not update the examination attempt.");
                    }
                }

                updateTopicPerformance(c, student, attempt);

                c.commit();

                return new EvaluationResult(
                        correct,
                        incorrect,
                        unanswered,
                        attempted,
                        total,
                        score,
                        percentage,
                        accuracy
                );

            } catch (SQLException | RuntimeException e) {
                rollback(c);
                throw wrap("Could not submit the examination.", e);

            } finally {
                auto(c);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Could not submit the examination.", e);
        }
    }

    private ExamAttempt findActive(
            Connection c, long student, long exam) throws SQLException {

        String sql = """
                SELECT attempt_id, student_id, exam_id, started_at,
                       submitted_at, score, percentage, accuracy, status
                FROM exam_attempts
                WHERE student_id = ?
                AND exam_id = ?
                AND status = 'IN_PROGRESS'
                FOR UPDATE
                """;

        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, student);
            s.setLong(2, exam);

            try (ResultSet r = s.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    private ExamAttempt findAttempt(
            Connection c, long id, long student) throws SQLException {

        String sql = """
                SELECT attempt_id, student_id, exam_id, started_at,
                       submitted_at, score, percentage, accuracy, status
                FROM exam_attempts
                WHERE attempt_id = ? AND student_id = ?
                """;

        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            s.setLong(2, student);

            try (ResultSet r = s.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    private ExamAttempt findAttemptForUpdate(
            Connection c, long id, long student) throws SQLException {

        String sql = """
                SELECT attempt_id, student_id, exam_id, started_at,
                       submitted_at, score, percentage, accuracy, status
                FROM exam_attempts
                WHERE attempt_id = ? AND student_id = ?
                FOR UPDATE
                """;

        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id);
            s.setLong(2, student);

            try (ResultSet r = s.executeQuery()) {
                return r.next() ? map(r) : null;
            }
        }
    }

    /**
     * Reads answers and converts MySQL numeric IDs safely to Long.
     */
    private List<Row> rows(Connection c, long attempt) throws SQLException {

        String sql = """
                SELECT q.question_id,
                       q.marks,
                       sa.selected_option_id,
                       COALESCE(o.is_correct, FALSE) AS correct

                FROM exam_attempts a
                JOIN exam_questions eq ON eq.exam_id = a.exam_id
                JOIN questions q ON q.question_id = eq.question_id

                LEFT JOIN student_answers sa
                    ON sa.attempt_id = a.attempt_id
                    AND sa.question_id = q.question_id

                LEFT JOIN question_options o
                    ON o.option_id = sa.selected_option_id
                    AND o.question_id = q.question_id

                WHERE a.attempt_id = ?
                """;

        List<Row> out = new ArrayList<>();

        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, attempt);

            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {

                    Object selectedOption = r.getObject(3);

                    // FIX: MySQL may return BigInteger instead of Long.
                    Long optionId = selectedOption == null
                            ? null
                            : ((Number) selectedOption).longValue();

                    out.add(new Row(
                            r.getLong(1),
                            r.getBigDecimal(2),
                            optionId,
                            r.getBoolean(4)
                    ));
                }
            }
        }

        return out;
    }

    private void updateAnswerEvaluation(
            Connection c, long attempt, Row row) throws SQLException {

        String sql = """
                UPDATE student_answers
                SET is_correct = ?, marks_obtained = ?
                WHERE attempt_id = ? AND question_id = ?
                """;

        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setBoolean(1, row.correct);
            s.setBigDecimal(
                    2,
                    row.correct ? row.marks : BigDecimal.ZERO
            );
            s.setLong(3, attempt);
            s.setLong(4, row.questionId);
            s.executeUpdate();
        }
    }

    private void updateTopicPerformance(
            Connection c, long student, long attempt) throws SQLException {

        String sql = """
                INSERT INTO topic_performance
                (student_id, topic_id, total_attempted, total_correct, accuracy)

                SELECT ?, q.topic_id,
                       COUNT(sa.answer_id),
                       COALESCE(SUM(sa.is_correct), 0),
                       ROUND(
                           COALESCE(SUM(sa.is_correct), 0) * 100 /
                           NULLIF(COUNT(sa.answer_id), 0),
                           2
                       )

                FROM student_answers sa
                JOIN questions q ON q.question_id = sa.question_id
                WHERE sa.attempt_id = ?
                GROUP BY q.topic_id

                ON DUPLICATE KEY UPDATE
                    total_attempted =
                        topic_performance.total_attempted + VALUES(total_attempted),

                    total_correct =
                        topic_performance.total_correct + VALUES(total_correct),

                    accuracy = ROUND(
                        (topic_performance.total_correct + VALUES(total_correct))
                        * 100 /
                        NULLIF(
                            topic_performance.total_attempted +
                            VALUES(total_attempted),
                            0
                        ),
                        2
                    )
                """;

        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, student);
            s.setLong(2, attempt);
            s.executeUpdate();
        }
    }

    private ExamAttempt map(ResultSet r) throws SQLException {

        ExamAttempt a = new ExamAttempt();

        a.setAttemptId(r.getLong("attempt_id"));
        a.setStudentId(r.getLong("student_id"));
        a.setExamId(r.getLong("exam_id"));
        a.setStartedAt(r.getTimestamp("started_at").toInstant());

        Timestamp submitted = r.getTimestamp("submitted_at");

        a.setSubmittedAt(
                submitted == null ? null : submitted.toInstant()
        );

        a.setScore(r.getBigDecimal("score"));
        a.setPercentage(r.getBigDecimal("percentage"));
        a.setAccuracy(r.getBigDecimal("accuracy"));
        a.setStatus(AttemptStatus.valueOf(r.getString("status")));

        return a;
    }

    private void rollback(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void auto(Connection c) {
        try {
            c.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }

    private DatabaseException wrap(String message, Exception e) {
        return e instanceof DatabaseException d
                ? d
                : new DatabaseException(message, e);
    }

    private record Row(
            long questionId,
            BigDecimal marks,
            Long optionId,
            boolean correct
    ) {
    }
}
