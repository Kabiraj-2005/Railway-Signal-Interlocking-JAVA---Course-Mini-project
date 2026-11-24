package com.trains.interlocking.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Opens JDBC connections using {@code db.url} from app.properties and creates the schema.
 */
public final class DatabaseConnection {

    private static final Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

    private DatabaseConnection() {
    }

    /** Opens a connection to the configured database. */
    public static Connection getConnection() throws SQLException {
        return getConnection(AppConfig.getDbUrl());
    }

    /** Opens a connection to the given JDBC url (used by the test harness). */
    public static Connection getConnection(String url) throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        LOG.info("Connected to " + url);
        return conn;
    }

    /**
     * Runs schema.sql: creates the tables if needed and inserts the demo station layout
     * (seed rows use INSERT OR IGNORE, so existing state is kept). Runs in one transaction.
     */
    public static void initialiseSchema(Connection conn) throws SQLException {
        String script = readSchema();
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            for (String sql : script.split(";")) {
                if (!sql.isBlank()) {
                    st.execute(sql.trim());
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    /** Reads schema.sql from the classpath with comment lines removed. */
    private static String readSchema() throws SQLException {
        InputStream in = DatabaseConnection.class.getResourceAsStream("/schema.sql");
        if (in == null) {
            throw new SQLException("schema.sql not found on classpath");
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("--")) {
                    sb.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            throw new SQLException("Could not read schema.sql", e);
        }
        return sb.toString();
    }
}
