/*
 * RouteDao.java
 * Loads routes from three tables (route, route_section, route_point) and saves a
 * route's state (active flag and train position). A route is built from the SAME
 * section, point and signal objects that are already in the layout map.
 * Lecture decks: JDBC (PreparedStatement, ResultSet), DAO pattern, Collections, casting.
 * Owner: Harsh
 */
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
import java.util.Optional;

public class RouteDao implements Dao<Route> {

    private final Connection conn;
    private final HashMap<String, TrackElement> layout; // to turn ids into objects

    public RouteDao(Connection conn, HashMap<String, TrackElement> layout) {
        this.conn = conn;
        this.layout = layout;
    }

    @Override
    public List<Route> findAll() throws SQLException {
        List<Route> routes = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM route ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                Signal signal = (Signal) layout.get(rs.getString("entry_signal_id"));
                Route route = new Route(id, rs.getString("description"), signal,
                        loadSections(id), loadPoints(id));
                route.setActive(rs.getInt("active") == 1);
                route.setTrainPosition(rs.getInt("train_position"));
                routes.add(route);
            }
        }
        return routes;
    }

    @Override
    public Optional<Route> findById(String id) throws SQLException {
        // There are only four routes, so searching the full list is simplest.
        for (Route route : findAll()) {
            if (route.getId().equals(id)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    /** Saves only the run-time state; the route's path never changes. */
    @Override
    public void update(Route route) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE route SET active = ?, train_position = ? WHERE id = ?")) {
            ps.setInt(1, route.isActive() ? 1 : 0);
            ps.setInt(2, route.getTrainPosition());
            ps.setString(3, route.getId());
            ps.executeUpdate();
        }
    }

    private ArrayList<TrackSection> loadSections(String routeId) throws SQLException {
        ArrayList<TrackSection> sections = new ArrayList<>();
        // ORDER BY position: the train must run over the sections in the right order.
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT section_id FROM route_section WHERE route_id = ? ORDER BY position")) {
            ps.setString(1, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sections.add((TrackSection) layout.get(rs.getString("section_id")));
                }
            }
        }
        return sections;
    }

    private HashMap<Point, Boolean> loadPoints(String routeId) throws SQLException {
        HashMap<Point, Boolean> points = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT point_id, reversed FROM route_point WHERE route_id = ?")) {
            ps.setString(1, routeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Point point = (Point) layout.get(rs.getString("point_id"));
                    points.put(point, rs.getInt("reversed") == 1);
                }
            }
        }
        return points;
    }
}
