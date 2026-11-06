
package com.adaptiq.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class DatabaseConnectionTest {

    public static void main(String[] args) {
        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            DatabaseMetaData metaData =
                    connection.getMetaData();

            System.out.println("================================");
            System.out.println("DATABASE CONNECTION SUCCESSFUL!");
            System.out.println("Database: "
                    + connection.getCatalog());
            System.out.println("MySQL Version: "
                    + metaData.getDatabaseProductVersion());
            System.out.println("Connected: "
                    + !connection.isClosed());
            System.out.println("================================");

        } catch (Exception e) {
            System.out.println("DATABASE CONNECTION FAILED!");
            e.printStackTrace();
        }
    }
}
