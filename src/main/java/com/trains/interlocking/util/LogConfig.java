package com.trains.interlocking.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Configures java.util.logging so that everything under {@code com.trains.interlocking}
 * is also written to the log file named in app.properties.
 */
public final class LogConfig {

    private static final String ROOT_LOGGER = "com.trains.interlocking";
    private static boolean configured;

    private LogConfig() {
    }

    public static synchronized void configure() {
        if (configured) {
            return;
        }
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$-7s %3$s - %5$s%6$s%n");
        // keep the console for real problems, the file gets everything
        for (Handler h : Logger.getLogger("").getHandlers()) {
            h.setLevel(Level.WARNING);
        }
        Logger logger = Logger.getLogger(ROOT_LOGGER);
        logger.setLevel(Level.INFO);
        try {
            FileHandler handler = new FileHandler(AppConfig.getLogFile(), true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException e) {
            logger.warning("Could not open log file: " + e.getMessage());
        }
        configured = true;
    }
}
