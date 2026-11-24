package com.trains.interlocking.dao;

import com.trains.interlocking.model.TrackSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code track_section} table.
 */
public class TrackSectionDao implements Dao<TrackSection> {

    private final Connection conn;

    public TrackSectionDao(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void save(TrackSection s) throws SQLException {
        String sql = "INSERT INTO track_section (id, description, occupied, locked) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getDescription());
            ps.setInt(3, s.isOccupied() ? 1 : 0);
            ps.setInt(4, s.isLocked() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<TrackSection> findById(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_section WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<TrackSection> findAll() throws SQLException {
        List<TrackSection> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_section ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public void update(TrackSection s) throws SQLException {
        String sql = "UPDATE track_section SET description = ?, occupied = ?, locked = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getDescription());
            ps.setInt(2, s.isOccupied() ? 1 : 0);
            ps.setInt(3, s.isLocked() ? 1 : 0);
            ps.setString(4, s.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No track section with id " + s.getId());
            }
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM track_section WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    private TrackSection map(ResultSet rs) throws SQLException {
        TrackSection s = new TrackSection(rs.getString("id"), rs.getString("description"));
        s.setOccupied(rs.getInt("occupied") == 1);
        if (rs.getInt("locked") == 1) {
            s.lock();
        }
        return s;
    }
}
