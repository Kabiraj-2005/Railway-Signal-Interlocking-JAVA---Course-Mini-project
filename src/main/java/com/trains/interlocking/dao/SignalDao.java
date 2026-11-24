/*
 * SignalDao.java
 * Reads and updates rows of the track_signal table using PreparedStatement only.
 * Lecture decks: JDBC (PreparedStatement, ResultSet), DAO pattern, Generics, Optional.
 * Owner: Harsh
 */
package com.trains.interlocking.dao;

import com.trains.interlocking.model.Signal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SignalDao implements Dao<Signal> {

    private final Connection conn;

    public SignalDao(Connection conn) {
        this.conn = conn;
    }

    @Override
    public List<Signal> findAll() throws SQLException {
        List<Signal> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_signal ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(fromRow(rs));
            }
        }
        return list;
    }

    @Override
    public Optional<Signal> findById(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_signal WHERE id = ?")) {
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
    public void update(Signal signal) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE track_signal SET clear = ? WHERE id = ?")) {
            ps.setInt(1, signal.isClear() ? 1 : 0); // SQLite has no boolean type
            ps.setString(2, signal.getId());
            ps.executeUpdate();
        }
    }

    /** Turns the current row of the ResultSet into a Signal object. */
    private Signal fromRow(ResultSet rs) throws SQLException {
        return new Signal(rs.getString("id"), rs.getInt("clear") == 1);
    }
}
