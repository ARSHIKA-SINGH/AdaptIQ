package com.adaptiq.dao;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Immutable audit-log writes used by security-sensitive operations. */
public final class ActivityLogDAO implements ActivityLogRepository {
    private final ConnectionProvider connectionProvider;

    public ActivityLogDAO() { this(DatabaseConnection::getConnection); }
    public ActivityLogDAO(ConnectionProvider connectionProvider) { this.connectionProvider = connectionProvider; }

    @Override
    public void record(Long userId, String activity) {
        String sql = "INSERT INTO activity_logs (user_id, activity) VALUES (?, ?)";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) statement.setNull(1, java.sql.Types.BIGINT); else statement.setLong(1, userId);
            statement.setString(2, activity);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not record system activity.", exception);
        }
    }
}
