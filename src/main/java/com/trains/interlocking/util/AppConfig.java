/*
 * AppConfig.java
 * Reads the settings file app.properties (database URL, station name, log file name)
 * once, when the class is first used, and hands the values out.
 * Lecture decks: Properties, static blocks, I/O Streams.
 * Owner: Harsh
 */
package com.trains.interlocking.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties SETTINGS = new Properties();

    // A static block runs once, the first time the class is used.
    static {
        // app.properties is inside the program's classpath (src/main/resources),
        // so we open it as a "resource" instead of with a file path.
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                SETTINGS.load(in);
            }
        } catch (IOException e) {
            System.out.println("Could not read app.properties: " + e.getMessage());
        }
    }

    public static String getDbUrl() {
        return SETTINGS.getProperty("db.url", "jdbc:sqlite:interlocking.db");
    }

    public static String getStationName() {
        return SETTINGS.getProperty("station.name", "Station");
    }

    public static String getLogFile() {
        return SETTINGS.getProperty("log.file", "interlocking.log");
    }
}
