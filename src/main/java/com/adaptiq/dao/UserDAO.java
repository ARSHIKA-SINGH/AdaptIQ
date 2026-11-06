package com.adaptiq.dao;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.model.UserStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

/** JDBC implementation for account records. All values are bound with PreparedStatement. */
public final class UserDAO implements UserAccountRepository {
    private static final String FIND_BY_EMAIL = """
            SELECT user_id, full_name, email, password_hash, role, status, created_at
            FROM users WHERE email = ?
            """;
    private static final String INSERT = """
            INSERT INTO users (full_name, email, password_hash, role, status)
            VALUES (?, ?, ?, ?, ?)
            """;
    private final ConnectionProvider connectionProvider;

    public UserDAO() { this(DatabaseConnection::getConnection); }
    public UserDAO(ConnectionProvider connectionProvider) { this.connectionProvider = connectionProvider; }

    @Override
    public Optional<User> findByEmail(String email) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_EMAIL)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve the account.", exception);
        }
    }

    @Override
    public boolean emailExists(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public long create(User user) {
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());
            statement.setString(4, user.getRole().name());
            statement.setString(5, user.getStatus().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DatabaseException("Account creation did not return an identifier.");
                }
                return keys.getLong(1);
            }
        } catch (SQLException exception) {
            // MySQL error 1062 still maps to a safe, deterministic client message.
            if (exception.getErrorCode() == 1062) {
                throw new DatabaseException("An account with that email already exists.", exception);
            }
            throw new DatabaseException("Could not create the account.", exception);
        }
    }

    private User map(ResultSet result) throws SQLException {
        Timestamp createdAt = result.getTimestamp("created_at");
        return new User(result.getLong("user_id"), result.getString("full_name"), result.getString("email"),
                result.getString("password_hash"), Role.valueOf(result.getString("role")),
                UserStatus.valueOf(result.getString("status")), createdAt == null ? null : createdAt.toInstant());
    }
}
