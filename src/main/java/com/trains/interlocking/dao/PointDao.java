/*
 * PointDao.java
 * Reads and updates rows of the track_point table using PreparedStatement only.
 * Lecture decks: JDBC (PreparedStatement, ResultSet), DAO pattern, Generics, Optional.
 * Owner: Harsh
 */
package com.trains.interlocking.dao;

import com.trains.interlocking.model.Point;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PointDao implements Dao<Point> {

    private final Connection conn;

    public PointDao(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<Point> findAll() throws SQLException {
        List<Point> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_point ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(fromRow(rs));
            }
        }
        return list;
    }

    @Override
    public Optional<Point> findById(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_point WHERE id = ?")) {
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
    public void update(Point point) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE track_point SET reversed = ?, locked = ? WHERE id = ?")) {
            ps.setInt(1, point.isReversed() ? 1 : 0); // SQLite has no boolean type
            ps.setInt(2, point.isLocked() ? 1 : 0);
            ps.setString(3, point.getId());
            ps.executeUpdate();
        }
    }

    /** Turns the current row of the ResultSet into a Point object. */
    private Point fromRow(ResultSet rs) throws SQLException {
        return new Point(rs.getString("id"), rs.getInt("reversed") == 1, rs.getInt("locked") == 1);
    }
}
