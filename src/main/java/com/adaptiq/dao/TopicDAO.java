package com.adaptiq.dao;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.Topic;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class TopicDAO {
    private final ConnectionProvider connections;
    public TopicDAO() { this(DatabaseConnection::getConnection); }
    public TopicDAO(ConnectionProvider connections) { this.connections = connections; }
    public List<Topic> findBySubject(long subjectId) {
        List<Topic> topics = new ArrayList<>();
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("SELECT topic_id, subject_id, topic_name, description FROM topics WHERE subject_id = ? ORDER BY topic_name")) {
            s.setLong(1, subjectId); try (ResultSet r = s.executeQuery()) { while (r.next()) topics.add(new Topic(r.getLong(1), r.getLong(2), r.getString(3), r.getString(4))); }
            return topics;
        } catch (SQLException e) { throw new DatabaseException("Could not retrieve topics.", e); }
    }
    public long create(Topic topic) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("INSERT INTO topics (subject_id, topic_name, description) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, topic.getSubjectId()); s.setString(2, topic.getTopicName()); s.setString(3, topic.getDescription()); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new DatabaseException("Topic creation did not return an identifier."); return keys.getLong(1); }
        } catch (SQLException e) { throw new DatabaseException(e.getErrorCode() == 1062 ? "A topic with that name already exists in this subject." : "Could not create the topic.", e); }
    }
    public void update(Topic topic) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE topics SET subject_id = ?, topic_name = ?, description = ? WHERE topic_id = ?")) {
            s.setLong(1, topic.getSubjectId()); s.setString(2, topic.getTopicName()); s.setString(3, topic.getDescription()); s.setLong(4, topic.getTopicId());
            if (s.executeUpdate() != 1) throw new DatabaseException("Topic no longer exists.");
        } catch (SQLException e) { throw new DatabaseException(e.getErrorCode() == 1062 ? "A topic with that name already exists in this subject." : "Could not update the topic.", e); }
    }
    public void delete(long id) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM topics WHERE topic_id = ?")) {
            s.setLong(1, id); if (s.executeUpdate() != 1) throw new DatabaseException("Topic no longer exists.");
        } catch (SQLException e) { throw new DatabaseException("This topic cannot be deleted while it has related records.", e); }
    }
}
