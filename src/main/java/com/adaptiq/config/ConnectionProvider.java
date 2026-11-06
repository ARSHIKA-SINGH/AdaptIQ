package com.adaptiq.config;

import java.sql.Connection;
import java.sql.SQLException;

/** Abstraction used by DAOs and integration tests to obtain JDBC connections. */
@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
}
