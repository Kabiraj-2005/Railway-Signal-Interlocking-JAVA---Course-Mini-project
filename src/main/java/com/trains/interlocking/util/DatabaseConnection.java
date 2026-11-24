/*
 * DatabaseConnection.java
 * Opens a JDBC connection to the SQLite database with DriverManager, and runs
 * schema.sql to create the tables and insert the demo station the first time.
 * Lecture decks: JDBC (DriverManager, Connection, PreparedStatement, transactions), I/O Streams.
 * Owner: Harsh
 */
package com.trains.interlocking.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnection {

    /** Opens the database named in app.properties. */
    public static Connection getConnection() throws SQLException {
        return getConnection(AppConfig.getDbUrl());
    }

    /** Opens any database URL (the tests use a temporary file). */
    public static Connection getConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }

    /** Runs every statement in schema.sql inside ONE transaction. */
    public static void initialiseSchema(Connection conn) throws SQLException {
        String[] statements = readSchema().split(";");
        conn.setAutoCommit(false);
        try {
            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.execute();
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback(); // all or nothing: never leave half a schema behind
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** Reads schema.sql, skipping comment lines that start with "--". */
    private static String readSchema() throws SQLException {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                DatabaseConnection.class.getResourceAsStream("/schema.sql")))) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().startsWith("--")) {
                    text.append(line).append("\n");
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new SQLException("Could not read schema.sql: " + e.getMessage());
        }
        return text.toString();
    }
}
