/*
 * LogConfig.java
 * Sets up java.util.logging so that every message from our packages is written to
 * the log file named in app.properties (interlocking.log).
 * Lecture decks: Logging (Logger, FileHandler, SimpleFormatter, Level).
 * Owner: Utpal
 */
package com.trains.interlocking.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogConfig {

    // Kept in a static field so the logger (and its file handler) is not garbage collected.
    private static final Logger APP_LOGGER = Logger.getLogger("com.trains.interlocking");
    private static boolean configured = false;

    public static void configure() {
        if (configured) {
            return; // only add the file handler once
        }
        APP_LOGGER.setLevel(Level.INFO);
        // Loggers of our classes (e.g. ...service.InterlockingService) are "children"
        // of this logger, so their messages also reach this handler.
        APP_LOGGER.setUseParentHandlers(false); // file only, keep the console clean
        try {
            FileHandler fileHandler = new FileHandler(AppConfig.getLogFile(), true); // true = append
            fileHandler.setFormatter(new SimpleFormatter());
            APP_LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            System.out.println("Could not open log file: " + e.getMessage());
        }
        configured = true;
    }
}
