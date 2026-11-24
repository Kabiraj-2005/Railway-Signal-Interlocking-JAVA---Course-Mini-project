/*
 * EventLogDao.java
 * Adds rows to the event_log table and reads the most recent ones back.
 * It is not a Dao<T> because log entries are only ever added, never updated.
 * Lecture decks: JDBC (PreparedStatement, ResultSet), Collections (LinkedList), LocalDateTime.
 * Owner: Harsh
 */
package com.trains.interlocking.dao;

import com.trains.interlocking.model.EventLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedList;

public class EventLogDao {

    private final Connection conn;

    public EventLogDao(Connection conn) {
        this.conn = conn;
    }

    public void insert(EventLogEntry entry) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO event_log (timestamp, route_id, action, message) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, entry.getTime().toString()); // stored as text, e.g. 2026-11-24T16:46:13
            ps.setString(2, entry.getRouteId());
            ps.setString(3, entry.getAction());
            ps.setString(4, entry.getMessage());
            ps.executeUpdate();
        }
    }

    /** The newest entries, returned oldest first. */
    public LinkedList<EventLogEntry> findRecent(int limit) throws SQLException {
        LinkedList<EventLogEntry> entries = new LinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM event_log ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EventLogEntry entry = new EventLogEntry(LocalDateTime.parse(rs.getString("timestamp")),
                            rs.getString("route_id"), rs.getString("action"), rs.getString("message"));
                    // Rows come newest first; adding each at the front reverses the order.
                    entries.addFirst(entry);
                }
            }
        }
        return entries;
    }
}
