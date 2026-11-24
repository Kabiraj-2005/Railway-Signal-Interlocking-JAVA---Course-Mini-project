/*
 * StationRepository.java
 * Loads the whole station from the database at start-up, and saves its state after
 * every change in ONE transaction (all rows are saved, or none are).
 * It implements the StationStore interface, so the service never sees any SQL.
 * Lecture decks: JDBC transactions (setAutoCommit, commit, rollback), DAO pattern, Interfaces.
 * Owner: Harsh
 */
package com.trains.interlocking.dao;

import com.trains.interlocking.model.EventLogEntry;
import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.Signal;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;
import com.trains.interlocking.service.StationStore;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StationRepository implements StationStore {

    private final Connection conn;
    private final TrackSectionDao sectionDao;
    private final PointDao pointDao;
    private final SignalDao signalDao;
    private final EventLogDao eventLogDao;

    public StationRepository(Connection conn) {
        this.conn = conn;
        this.sectionDao = new TrackSectionDao(conn);
        this.pointDao = new PointDao(conn);
        this.signalDao = new SignalDao(conn);
        this.eventLogDao = new EventLogDao(conn);
    }

    public HashMap<String, TrackElement> loadLayout() throws SQLException {
        HashMap<String, TrackElement> layout = new HashMap<>();
        for (TrackSection section : sectionDao.findAll()) {
            layout.put(section.getId(), section);
        }
        for (Point point : pointDao.findAll()) {
            layout.put(point.getId(), point);
        }
        for (Signal signal : signalDao.findAll()) {
            layout.put(signal.getId(), signal);
        }
        return layout;
    }

    public ArrayList<Route> loadRoutes(HashMap<String, TrackElement> layout) throws SQLException {
        return new ArrayList<>(new RouteDao(conn, layout).findAll());
    }

    @Override
    public void saveState(List<TrackElement> elements, List<Route> routes) throws SQLException {
        conn.setAutoCommit(false); // start the transaction
        try {
            for (TrackElement element : elements) {
                // Each kind of element lives in its own table, so pick the matching DAO.
                if (element instanceof TrackSection) {
                    sectionDao.update((TrackSection) element);
                } else if (element instanceof Point) {
                    pointDao.update((Point) element);
                } else {
                    signalDao.update((Signal) element);
                }
            }
            RouteDao routeDao = new RouteDao(conn, null); // update() needs no layout
            for (Route route : routes) {
                routeDao.update(route);
            }
            conn.commit(); // everything worked: make it permanent
        } catch (SQLException e) {
            conn.rollback(); // something failed: undo every change since setAutoCommit(false)
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    @Override
    public void logEvent(EventLogEntry entry) throws SQLException {
        eventLogDao.insert(entry);
    }

    public List<EventLogEntry> recentEvents(int limit) throws SQLException {
        return eventLogDao.findRecent(limit);
    }
}
