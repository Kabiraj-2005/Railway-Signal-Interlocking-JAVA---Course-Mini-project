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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the whole station from the database and saves its state back, using the DAOs.
 * Saving runs in one transaction so the stored layout is never half-updated.
 */
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

    /** @return every section, point and signal keyed by id. */
    public Map<String, TrackElement> loadLayout() throws SQLException {
        Map<String, TrackElement> layout = new LinkedHashMap<>();
        for (TrackSection s : sectionDao.findAll()) {
            layout.put(s.getId(), s);
        }
        for (Point p : pointDao.findAll()) {
            layout.put(p.getId(), p);
        }
        for (Signal s : signalDao.findAll()) {
            layout.put(s.getId(), s);
        }
        return layout;
    }

    /** Loads routes, resolving their elements against the given layout. */
    public List<Route> loadRoutes(Map<String, TrackElement> layout) throws SQLException {
        return new RouteDao(conn, layout).findAll();
    }

    @Override
    public void saveState(Collection<TrackElement> elements, Collection<Route> routes) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            for (TrackElement e : elements) {
                if (e instanceof TrackSection) {
                    sectionDao.update((TrackSection) e);
                } else if (e instanceof Point) {
                    pointDao.update((Point) e);
                } else if (e instanceof Signal) {
                    signalDao.update((Signal) e);
                }
            }
            RouteDao routeDao = new RouteDao(conn, null);
            for (Route r : routes) {
                routeDao.updateState(r);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
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
