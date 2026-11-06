package com.adaptiq.dao;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.Difficulty;
import com.adaptiq.model.Question;
import com.adaptiq.model.QuestionOption;
import java.sql.*;
import java.util.*;

/** Transactional persistence of questions and their normalised answer options. */
public final class QuestionDAO implements QuestionRepository {
    private final ConnectionProvider connections;
    public QuestionDAO() { this(DatabaseConnection::getConnection); }
    public QuestionDAO(ConnectionProvider connections) { this.connections = connections; }
    @Override public List<Question> findAll(QuestionFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT q.question_id, q.topic_id, t.topic_name, q.question_text, q.difficulty, q.marks, q.explanation, q.created_by, q.created_at FROM questions q JOIN topics t ON t.topic_id = q.topic_id WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        if (filter.subjectId() != null) { sql.append(" AND t.subject_id = ?"); parameters.add(filter.subjectId()); }
        if (filter.topicId() != null) { sql.append(" AND q.topic_id = ?"); parameters.add(filter.topicId()); }
        if (filter.difficulty() != null) { sql.append(" AND q.difficulty = ?"); parameters.add(filter.difficulty().name()); }
        if (filter.searchText() != null && !filter.searchText().isBlank()) { sql.append(" AND q.question_text LIKE ?"); parameters.add("%" + filter.searchText().trim() + "%"); }
        sql.append(" ORDER BY q.created_at DESC");
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement(sql.toString())) {
            bind(s, parameters); try (ResultSet r = s.executeQuery()) { List<Question> questions = new ArrayList<>(); while (r.next()) questions.add(map(c, r)); return questions; }
        } catch (SQLException e) { throw new DatabaseException("Could not retrieve questions.", e); }
    }
    @Override public Optional<Question> findById(long questionId) {
        String sql = "SELECT q.question_id, q.topic_id, t.topic_name, q.question_text, q.difficulty, q.marks, q.explanation, q.created_by, q.created_at FROM questions q JOIN topics t ON t.topic_id = q.topic_id WHERE q.question_id = ?";
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, questionId); try (ResultSet r = s.executeQuery()) { return r.next() ? Optional.of(map(c, r)) : Optional.empty(); } }
        catch (SQLException e) { throw new DatabaseException("Could not retrieve the question.", e); }
    }
    @Override public long create(Question question) {
        try (Connection c = connections.getConnection()) { c.setAutoCommit(false); try {
            long id = insertQuestion(c, question); insertOptions(c, id, question.getOptions()); c.commit(); return id;
        } catch (SQLException | RuntimeException e) { rollback(c); throw wrap("Could not create the question.", e); } finally { restoreAutoCommit(c); } }
        catch (SQLException e) { throw new DatabaseException("Could not create the question.", e); }
    }
    @Override public void update(Question question) {
        try (Connection c = connections.getConnection()) { c.setAutoCommit(false); try {
            try (PreparedStatement s = c.prepareStatement("UPDATE questions SET topic_id = ?, question_text = ?, difficulty = ?, marks = ?, explanation = ? WHERE question_id = ?")) { bindQuestion(s, question); s.setLong(6, question.getQuestionId()); if (s.executeUpdate() != 1) throw new DatabaseException("Question no longer exists."); }
            try (PreparedStatement s = c.prepareStatement("DELETE FROM question_options WHERE question_id = ?")) { s.setLong(1, question.getQuestionId()); s.executeUpdate(); }
            insertOptions(c, question.getQuestionId(), question.getOptions()); c.commit();
        } catch (SQLException | RuntimeException e) { rollback(c); throw wrap("Could not update the question.", e); } finally { restoreAutoCommit(c); } }
        catch (SQLException e) { throw new DatabaseException("Could not update the question.", e); }
    }
    @Override public void delete(long questionId) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM questions WHERE question_id = ?")) { s.setLong(1, questionId); if (s.executeUpdate() != 1) throw new DatabaseException("Question no longer exists."); }
        catch (SQLException e) { throw new DatabaseException("This question cannot be deleted while it is assigned to an examination or attempt.", e); }
    }
    private long insertQuestion(Connection c, Question q) throws SQLException { try (PreparedStatement s = c.prepareStatement("INSERT INTO questions (topic_id, question_text, difficulty, marks, explanation, created_by) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) { bindQuestion(s, q); s.setLong(6, q.getCreatedBy()); s.executeUpdate(); try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new DatabaseException("Question creation did not return an identifier."); return keys.getLong(1); } } }
    private void bindQuestion(PreparedStatement s, Question q) throws SQLException { s.setLong(1, q.getTopicId()); s.setString(2, q.getQuestionText()); s.setString(3, q.getDifficulty().name()); s.setBigDecimal(4, q.getMarks()); s.setString(5, q.getExplanation()); }
    private void insertOptions(Connection c, long questionId, List<QuestionOption> options) throws SQLException { try (PreparedStatement s = c.prepareStatement("INSERT INTO question_options (question_id, option_label, option_text, is_correct) VALUES (?, ?, ?, ?)")) { for (QuestionOption o : options) { s.setLong(1, questionId); s.setString(2, o.getOptionLabel()); s.setString(3, o.getOptionText()); s.setBoolean(4, o.isCorrect()); s.addBatch(); } s.executeBatch(); } }
    private Question map(Connection c, ResultSet r) throws SQLException { Question q = new Question(); q.setQuestionId(r.getLong("question_id")); q.setTopicId(r.getLong("topic_id")); q.setTopicName(r.getString("topic_name")); q.setQuestionText(r.getString("question_text")); q.setDifficulty(Difficulty.valueOf(r.getString("difficulty"))); q.setMarks(r.getBigDecimal("marks")); q.setExplanation(r.getString("explanation")); q.setCreatedBy(r.getLong("created_by")); q.setCreatedAt(r.getTimestamp("created_at").toInstant()); q.setOptions(loadOptions(c, q.getQuestionId())); return q; }
    private List<QuestionOption> loadOptions(Connection c, long questionId) throws SQLException { List<QuestionOption> options = new ArrayList<>(); try (PreparedStatement s = c.prepareStatement("SELECT option_id, question_id, option_label, option_text, is_correct FROM question_options WHERE question_id = ? ORDER BY option_label")) { s.setLong(1, questionId); try (ResultSet r = s.executeQuery()) { while (r.next()) options.add(new QuestionOption(r.getLong(1), r.getLong(2), r.getString(3), r.getString(4), r.getBoolean(5))); } } return options; }
    private void bind(PreparedStatement s, List<Object> params) throws SQLException { for (int i=0;i<params.size();i++) s.setObject(i+1, params.get(i)); }
    private void rollback(Connection c) { try { c.rollback(); } catch (SQLException ignored) { } }
    private void restoreAutoCommit(Connection c) { try { c.setAutoCommit(true); } catch (SQLException ignored) { } }
    private DatabaseException wrap(String message, Exception e) { return e instanceof DatabaseException db ? db : new DatabaseException(message, e); }
}
