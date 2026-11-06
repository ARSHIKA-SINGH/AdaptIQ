package com.adaptiq.config;

import com.adaptiq.exception.DatabaseException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads local database credentials without embedding secrets in source code. */
public final class DatabaseConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final Path LOCAL_CONFIGURATION = Path.of("config", "application.properties");

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        DatabaseSettings settings = loadSettings();
        try {
            Connection connection = DriverManager.getConnection(settings.url(), settings.username(), settings.password());
            LOGGER.debug("Opened a database connection to AdaptIQ.");
            return connection;
        } catch (SQLException exception) {
            LOGGER.error("Database connection failed: {}", exception.getMessage());
            throw exception;
        }
    }

    private static DatabaseSettings loadSettings() {
        Properties properties = new Properties();
        if (Files.isRegularFile(LOCAL_CONFIGURATION)) {
            try (InputStream input = Files.newInputStream(LOCAL_CONFIGURATION)) {
                properties.load(input);
            } catch (IOException exception) {
                throw new DatabaseException("Unable to read local database configuration.", exception);
            }
        }

        String url = firstUsable(System.getenv("ADAPTIQ_DB_URL"), properties.getProperty("db.url"));
        String username = firstUsable(System.getenv("ADAPTIQ_DB_USERNAME"), properties.getProperty("db.username"));
        String password = firstUsable(System.getenv("ADAPTIQ_DB_PASSWORD"), properties.getProperty("db.password"));
        if (url == null || username == null || password == null) {
            throw new DatabaseException("Database credentials are missing. Create config/application.properties "
                    + "from application.properties.example or set ADAPTIQ_DB_URL, ADAPTIQ_DB_USERNAME, and ADAPTIQ_DB_PASSWORD.");
        }
        return new DatabaseSettings(url, username, password);
    }

    private static String firstUsable(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private record DatabaseSettings(String url, String username, String password) { }
}
