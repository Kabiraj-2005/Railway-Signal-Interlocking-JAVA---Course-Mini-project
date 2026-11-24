package com.trains.interlocking.dao;

import com.trains.interlocking.exception.PointLockedException;
import com.trains.interlocking.model.Point;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code track_point} table.
 */
public class PointDao implements Dao<Point> {

    private final Connection conn;

    public PointDao(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void save(Point p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO track_point (id, reversed, locked) VALUES (?, ?, ?)")) {
            ps.setString(1, p.getId());
            ps.setInt(2, p.isReversed() ? 1 : 0);
            ps.setInt(3, p.isLocked() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Point> findById(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_point WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Point> findAll() throws SQLException {
        List<Point> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_point ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public void update(Point p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE track_point SET reversed = ?, locked = ? WHERE id = ?")) {
            ps.setInt(1, p.isReversed() ? 1 : 0);
            ps.setInt(2, p.isLocked() ? 1 : 0);
            ps.setString(3, p.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No point with id " + p.getId());
            }
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM track_point WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    private Point map(ResultSet rs) throws SQLException {
        Point p = new Point(rs.getString("id"));
        try {
            // a freshly created point is unlocked, so it can always be moved here
            p.setReversed(rs.getInt("reversed") == 1);
        } catch (PointLockedException e) {
            throw new SQLException(e.getMessage(), e);
        }
        if (rs.getInt("locked") == 1) {
            p.lock();
        }
        return p;
    }
}
