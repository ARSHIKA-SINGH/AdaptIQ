package com.adaptiq.dao;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.Subject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class SubjectDAO {
    private final ConnectionProvider connections;
    public SubjectDAO() { this(DatabaseConnection::getConnection); }
    public SubjectDAO(ConnectionProvider connections) { this.connections = connections; }
    public List<Subject> findAll() {
        List<Subject> subjects = new ArrayList<>();
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("SELECT subject_id, subject_name, description FROM subjects ORDER BY subject_name"); ResultSet r = s.executeQuery()) {
            while (r.next()) subjects.add(new Subject(r.getLong(1), r.getString(2), r.getString(3)));
            return subjects;
        } catch (SQLException e) { throw new DatabaseException("Could not retrieve subjects.", e); }
    }
    public long create(Subject subject) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("INSERT INTO subjects (subject_name, description) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, subject.getSubjectName()); s.setString(2, subject.getDescription()); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new DatabaseException("Subject creation did not return an identifier."); return keys.getLong(1); }
        } catch (SQLException e) { throw new DatabaseException(e.getErrorCode() == 1062 ? "A subject with that name already exists." : "Could not create the subject.", e); }
    }
    public void update(Subject subject) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE subjects SET subject_name = ?, description = ? WHERE subject_id = ?")) {
            s.setString(1, subject.getSubjectName()); s.setString(2, subject.getDescription()); s.setLong(3, subject.getSubjectId());
            if (s.executeUpdate() != 1) throw new DatabaseException("Subject no longer exists.");
        } catch (SQLException e) { throw new DatabaseException(e.getErrorCode() == 1062 ? "A subject with that name already exists." : "Could not update the subject.", e); }
    }
    public void delete(long id) {
        try (Connection c = connections.getConnection(); PreparedStatement s = c.prepareStatement("DELETE FROM subjects WHERE subject_id = ?")) {
            s.setLong(1, id); if (s.executeUpdate() != 1) throw new DatabaseException("Subject no longer exists.");
        } catch (SQLException e) { throw new DatabaseException("This subject cannot be deleted while it has related records.", e); }
    }
}
