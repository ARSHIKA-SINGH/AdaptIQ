package com.adaptiq.dao;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.Difficulty;
import com.adaptiq.model.dashboard.AdminDashboardData;
import com.adaptiq.model.dashboard.AttemptSummary;
import com.adaptiq.model.dashboard.FacultyDashboardData;
import com.adaptiq.model.dashboard.RecommendationSummary;
import com.adaptiq.model.dashboard.StudentDashboardData;
import com.adaptiq.model.dashboard.TopicInsight;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Dashboard projections sourced entirely from persisted MySQL records. */
public final class DashboardDAO implements DashboardRepository {
    private final ConnectionProvider connectionProvider;

    public DashboardDAO() { this(DatabaseConnection::getConnection); }
    public DashboardDAO(ConnectionProvider connectionProvider) { this.connectionProvider = connectionProvider; }

    @Override
    public StudentDashboardData loadStudentDashboard(long studentId) {
        String metrics = """
                SELECT COUNT(*) AS total_attempts, COALESCE(AVG(percentage), 0) AS average_percentage,
                       COALESCE(AVG(accuracy), 0) AS overall_accuracy, COALESCE(MAX(score), 0) AS highest_score
                FROM exam_attempts WHERE student_id = ? AND status IN ('SUBMITTED', 'EXPIRED')
                """;
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(metrics)) {
            statement.setLong(1, studentId);
            long totalAttempts;
            BigDecimal averagePercentage;
            BigDecimal overallAccuracy;
            BigDecimal highestScore;
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new DatabaseException("Could not load student dashboard metrics.");
                totalAttempts = result.getLong("total_attempts");
                averagePercentage = result.getBigDecimal("average_percentage");
                overallAccuracy = result.getBigDecimal("overall_accuracy");
                highestScore = result.getBigDecimal("highest_score");
            }
            return new StudentDashboardData(totalAttempts, averagePercentage, overallAccuracy, highestScore,
                    loadRecentAttempts(connection, studentId), loadTopicInsights(connection, studentId, true),
                    loadTopicInsights(connection, studentId, false), loadRecommendations(connection, studentId));
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load student dashboard.", exception);
        }
    }

    @Override
    public FacultyDashboardData loadFacultyDashboard(long facultyId) {
        String sql = """
                SELECT COUNT(DISTINCT e.exam_id) AS total_exams,
                       COUNT(DISTINCT CASE WHEN e.status = 'PUBLISHED' THEN e.exam_id END) AS published_exams,
                       COUNT(a.attempt_id) AS total_attempts, COALESCE(AVG(a.percentage), 0) AS average_percentage
                FROM exams e LEFT JOIN exam_attempts a ON a.exam_id = e.exam_id AND a.status IN ('SUBMITTED', 'EXPIRED')
                WHERE e.faculty_id = ?
                """;
        try (Connection connection = connectionProvider.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, facultyId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new DatabaseException("Could not load faculty dashboard metrics.");
                return new FacultyDashboardData(result.getLong("total_exams"), result.getLong("published_exams"),
                        result.getLong("total_attempts"), result.getBigDecimal("average_percentage"));
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load faculty dashboard.", exception);
        }
    }

    @Override
    public AdminDashboardData loadAdminDashboard() {
        String sql = """
                SELECT (SELECT COUNT(*) FROM users WHERE role = 'STUDENT') AS total_students,
                       (SELECT COUNT(*) FROM users WHERE role = 'FACULTY') AS total_faculty,
                       (SELECT COUNT(*) FROM exams) AS total_exams,
                       (SELECT COUNT(*) FROM questions) AS total_questions
                """;
        try (Connection connection = connectionProvider.getConnection(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) throw new DatabaseException("Could not load administrator dashboard metrics.");
            return new AdminDashboardData(result.getLong("total_students"), result.getLong("total_faculty"),
                    result.getLong("total_exams"), result.getLong("total_questions"));
        } catch (SQLException exception) {
            throw new DatabaseException("Could not load administrator dashboard.", exception);
        }
    }

    private List<AttemptSummary> loadRecentAttempts(Connection connection, long studentId) throws SQLException {
        String sql = """
                SELECT e.title, a.submitted_at, a.percentage FROM exam_attempts a
                JOIN exams e ON e.exam_id = a.exam_id
                WHERE a.student_id = ? AND a.status IN ('SUBMITTED', 'EXPIRED') AND a.submitted_at IS NOT NULL
                ORDER BY a.submitted_at DESC LIMIT 5
                """;
        List<AttemptSummary> attempts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) attempts.add(new AttemptSummary(result.getString("title"),
                        result.getTimestamp("submitted_at").toInstant(), result.getBigDecimal("percentage")));
            }
        }
        return attempts;
    }

    private List<TopicInsight> loadTopicInsights(Connection connection, long studentId, boolean weakest) throws SQLException {
        String direction = weakest ? "ASC" : "DESC";
        String sql = "SELECT t.topic_name, p.accuracy FROM topic_performance p JOIN topics t ON t.topic_id = p.topic_id "
                + "WHERE p.student_id = ? AND p.total_attempted > 0 ORDER BY p.accuracy " + direction + ", t.topic_name ASC LIMIT 3";
        List<TopicInsight> topics = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) topics.add(new TopicInsight(result.getString("topic_name"), result.getBigDecimal("accuracy")));
            }
        }
        return topics;
    }

    private List<RecommendationSummary> loadRecommendations(Connection connection, long studentId) throws SQLException {
        String sql = """
                SELECT t.topic_name, r.recommendation_text, r.recommended_difficulty FROM recommendations r
                JOIN topics t ON t.topic_id = r.topic_id
                WHERE r.student_id = ? AND r.status = 'ACTIVE' ORDER BY r.created_at DESC LIMIT 3
                """;
        List<RecommendationSummary> recommendations = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, studentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) recommendations.add(new RecommendationSummary(result.getString("topic_name"),
                        result.getString("recommendation_text"), Difficulty.valueOf(result.getString("recommended_difficulty"))));
            }
        }
        return recommendations;
    }
}
