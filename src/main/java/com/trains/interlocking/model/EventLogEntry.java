/*
 * EventLogEntry.java
 * One line of the event log: when it happened, which route, what kind of action
 * (SET, MOVE, REFUSED ...) and a message. Stored in the event_log table.
 * Lecture decks: Classes and Objects, Date/Time API (LocalDateTime).
 * Owner: Utpal
 */
package com.trains.interlocking.model;

import java.time.LocalDateTime;

public class EventLogEntry {

    private final LocalDateTime time;
    private final String routeId;
    private final String action;
    private final String message;

    public EventLogEntry(LocalDateTime time, String routeId, String action, String message) {
        this.time = time;
        this.routeId = routeId;
        this.action = action;
        this.message = message;
    }

    public LocalDateTime getTime() {
        return time;
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
        // The time is created without nanoseconds, so this prints e.g. 2026-11-24T16:46:13
        return time + "  " + action + "  " + routeId + "  " + message;
    }
}
