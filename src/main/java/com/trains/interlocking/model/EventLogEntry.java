package com.trains.interlocking.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One line of the interlocking event log (route set, train moved, request refused ...).
 */
public class EventLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final String routeId;
    private final String action;
    private final String message;

    public EventLogEntry(LocalDateTime timestamp, String routeId, String action, String message) {
        this.timestamp = timestamp;
        this.routeId = routeId == null ? "" : routeId;
        this.action = action;
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTimestampText() {
        return timestamp.format(FORMAT);
    }

    public String getRouteId() {
        return routeId;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return getTimestampText() + " [" + action + "] " + routeId + " " + message;
    }
}
