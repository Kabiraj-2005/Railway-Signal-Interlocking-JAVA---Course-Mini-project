package com.trains.interlocking.dao;

import com.trains.interlocking.model.EventLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO for the append-only {@code event_log} table.
 */
public class EventLogDao {

    private final Connection conn;

    public EventLogDao(Connection conn) {
        this.conn = conn;
    }

    public void insert(EventLogEntry entry) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO event_log (timestamp, route_id, action, message) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, entry.getTimestamp().toString());
            ps.setString(2, entry.getRouteId());
            ps.setString(3, entry.getAction());
            ps.setString(4, entry.getMessage());
            ps.executeUpdate();
        }
    }

    /** @return the newest {@code limit} entries, oldest first. */
    public List<EventLogEntry> findRecent(int limit) throws SQLException {
        List<EventLogEntry> newestFirst = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM event_log ORDER BY id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    newestFirst.add(new EventLogEntry(
                            LocalDateTime.parse(rs.getString("timestamp")),
                            rs.getString("route_id"),
                            rs.getString("action"),
                            rs.getString("message")));
                }
            }
        }
        List<EventLogEntry> oldestFirst = new ArrayList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            oldestFirst.add(newestFirst.get(i));
        }
        return oldestFirst;
    }

    public int count() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM event_log");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
