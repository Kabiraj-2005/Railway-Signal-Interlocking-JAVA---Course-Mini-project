/*
 * TrackSectionDao.java
 * Reads and updates rows of the track_section table using PreparedStatement only.
 * Lecture decks: JDBC (PreparedStatement, ResultSet), DAO pattern, Generics, Optional.
 * Owner: Harsh
 */
package com.trains.interlocking.dao;

import com.trains.interlocking.model.TrackSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrackSectionDao implements Dao<TrackSection> {

    private final Connection conn;

    public TrackSectionDao(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<TrackSection> findAll() throws SQLException {
        List<TrackSection> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_section ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(fromRow(rs));
            }
        }
        return list;
    }

    @Override
    public Optional<TrackSection> findById(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_section WHERE id = ?")) {
            ps.setString(1, id); // "?" placeholders keep values separate from the SQL
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(fromRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public void update(TrackSection section) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE track_section SET occupied = ?, locked = ? WHERE id = ?")) {
            ps.setInt(1, section.isOccupied() ? 1 : 0); // SQLite has no boolean type
            ps.setInt(2, section.isLocked() ? 1 : 0);
            ps.setString(3, section.getId());
            ps.executeUpdate();
        }
    }

    /** Turns the current row of the ResultSet into a TrackSection object. */
    private TrackSection fromRow(ResultSet rs) throws SQLException {
        return new TrackSection(rs.getString("id"), rs.getInt("occupied") == 1, rs.getInt("locked") == 1);
    }
}
