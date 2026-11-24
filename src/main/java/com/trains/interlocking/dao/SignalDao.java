package com.trains.interlocking.dao;

import com.trains.interlocking.model.Signal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC DAO for the {@code track_signal} table.
 */
public class SignalDao implements Dao<Signal> {

    private final Connection conn;

    public SignalDao(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void save(Signal s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO track_signal (id, clear, locked) VALUES (?, ?, ?)")) {
            ps.setString(1, s.getId());
            ps.setInt(2, s.isClear() ? 1 : 0);
            ps.setInt(3, s.isLocked() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<Signal> findById(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_signal WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Signal> findAll() throws SQLException {
        List<Signal> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM track_signal ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public void update(Signal s) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE track_signal SET clear = ?, locked = ? WHERE id = ?")) {
            ps.setInt(1, s.isClear() ? 1 : 0);
            ps.setInt(2, s.isLocked() ? 1 : 0);
            ps.setString(3, s.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No signal with id " + s.getId());
            }
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM track_signal WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    private Signal map(ResultSet rs) throws SQLException {
        Signal s = new Signal(rs.getString("id"));
        s.setClear(rs.getInt("clear") == 1);
        if (rs.getInt("locked") == 1) {
            s.lock();
        }
        return s;
    }
}
