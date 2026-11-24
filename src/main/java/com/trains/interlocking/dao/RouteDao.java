package com.trains.interlocking.dao;

import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.Signal;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC DAO for routes ({@code route}, {@code route_section}, {@code route_point}).
 * Routes reference layout elements, so the DAO needs the already-loaded layout to
 * resolve ids into objects. Multi-table writes run in a transaction.
 */
public class RouteDao implements Dao<Route> {

    private final Connection conn;
    private final Map<String, TrackElement> layout;

    public RouteDao(Connection conn, Map<String, TrackElement> layout) {
        this.conn = conn;
        this.layout = layout;
    }

    @Override
    public void save(Route r) throws SQLException {
        boolean ownTx = beginIfNeeded();
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO route (id, description, entry_signal_id, active, train_position) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, r.getId());
                ps.setString(2, r.getDescription());
                ps.setString(3, r.getEntrySignal().getId());
                ps.setInt(4, r.isActive() ? 1 : 0);
                ps.setInt(5, r.getTrainPosition());
                ps.executeUpdate();
            }
            insertChildren(r);
            commitIfOwn(ownTx);
        } catch (SQLException e) {
            rollbackIfOwn(ownTx);
            throw e;
        }
    }

    @Override
    public Optional<Route> findById(String id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM route WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Route> findAll() throws SQLException {
        List<Route> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM route ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    @Override
    public void update(Route r) throws SQLException {
        boolean ownTx = beginIfNeeded();
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE route SET description = ?, entry_signal_id = ?, active = ?, train_position = ? WHERE id = ?")) {
                ps.setString(1, r.getDescription());
                ps.setString(2, r.getEntrySignal().getId());
                ps.setInt(3, r.isActive() ? 1 : 0);
                ps.setInt(4, r.getTrainPosition());
                ps.setString(5, r.getId());
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("No route with id " + r.getId());
                }
            }
            deleteChildren(r.getId());
            insertChildren(r);
            commitIfOwn(ownTx);
        } catch (SQLException e) {
            rollbackIfOwn(ownTx);
            throw e;
        }
    }

    /** Updates only the run-time state (active flag and train position). */
    public void updateState(Route r) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE route SET active = ?, train_position = ? WHERE id = ?")) {
            ps.setInt(1, r.isActive() ? 1 : 0);
            ps.setInt(2, r.getTrainPosition());
            ps.setString(3, r.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String id) throws SQLException {
        boolean ownTx = beginIfNeeded();
        try {
            deleteChildren(id);
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM route WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
            commitIfOwn(ownTx);
        } catch (SQLException e) {
            rollbackIfOwn(ownTx);
            throw e;
        }
    }

    private Route map(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        Signal signal = lookup(rs.getString("entry_signal_id"), Signal.class);

        List<TrackSection> sections = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT section_id FROM route_section WHERE route_id = ? ORDER BY seq")) {
            ps.setString(1, id);
            try (ResultSet srs = ps.executeQuery()) {
                while (srs.next()) {
                    sections.add(lookup(srs.getString("section_id"), TrackSection.class));
                }
            }
        }

        Map<Point, Boolean> points = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT point_id, reversed FROM route_point WHERE route_id = ?")) {
            ps.setString(1, id);
            try (ResultSet prs = ps.executeQuery()) {
                while (prs.next()) {
                    points.put(lookup(prs.getString("point_id"), Point.class), prs.getInt("reversed") == 1);
                }
            }
        }

        Route route = new Route(id, rs.getString("description"), signal, sections, points);
        route.setActive(rs.getInt("active") == 1);
        route.setTrainPosition(rs.getInt("train_position"));
        return route;
    }

    private <E extends TrackElement> E lookup(String elementId, Class<E> type) throws SQLException {
        TrackElement e = layout.get(elementId);
        if (!type.isInstance(e)) {
            throw new SQLException("Layout has no " + type.getSimpleName() + " with id " + elementId);
        }
        return type.cast(e);
    }

    private void insertChildren(Route r) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO route_section (route_id, seq, section_id) VALUES (?, ?, ?)")) {
            List<TrackSection> sections = r.getSections();
            for (int i = 0; i < sections.size(); i++) {
                ps.setString(1, r.getId());
                ps.setInt(2, i);
                ps.setString(3, sections.get(i).getId());
                ps.executeUpdate();
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO route_point (route_id, point_id, reversed) VALUES (?, ?, ?)")) {
            for (Map.Entry<Point, Boolean> e : r.getRequiredPointPositions().entrySet()) {
                ps.setString(1, r.getId());
                ps.setString(2, e.getKey().getId());
                ps.setInt(3, e.getValue() ? 1 : 0);
                ps.executeUpdate();
            }
        }
    }

    private void deleteChildren(String routeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM route_section WHERE route_id = ?")) {
            ps.setString(1, routeId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM route_point WHERE route_id = ?")) {
            ps.setString(1, routeId);
            ps.executeUpdate();
        }
    }

    /** Starts a transaction unless the caller already has one open. */
    private boolean beginIfNeeded() throws SQLException {
        if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
            return true;
        }
        return false;
    }

    private void commitIfOwn(boolean ownTx) throws SQLException {
        if (ownTx) {
            conn.commit();
            conn.setAutoCommit(true);
        }
    }

    private void rollbackIfOwn(boolean ownTx) throws SQLException {
        if (ownTx) {
            conn.rollback();
            conn.setAutoCommit(true);
        }
    }
}
