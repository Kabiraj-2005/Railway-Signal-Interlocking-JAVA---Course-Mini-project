package com.trains.interlocking.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads settings from {@code app.properties} on the classpath.
 * A JVM system property with the same key overrides the file (e.g. -Ddb.url=...).
 */
public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            System.err.println("Could not read app.properties: " + e.getMessage());
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) {
            return override;
        }
        return PROPS.getProperty(key, defaultValue);
    }

    public static String getDbUrl() {
        return get("db.url", "jdbc:sqlite:interlocking.db");
    }

    public static String getStationName() {
        return get("station.name", "Station");
    }

    public static String getLogFile() {
        return get("log.file", "interlocking.log");
    }
}
