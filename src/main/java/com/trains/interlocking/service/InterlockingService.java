package com.trains.interlocking.service;

import com.trains.interlocking.exception.PointLockedException;
import com.trains.interlocking.exception.RouteConflictException;
import com.trains.interlocking.exception.SectionOccupiedException;
import com.trains.interlocking.model.EventLogEntry;
import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.Signal;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Shared contract between the interlocking logic (Utpal), the database layer (Harsh)
 * and the UI (Aryan). The UI calls only these methods.
 *
 * <p>Interlocking rules enforced by this service:
 * <ul>
 *   <li>A route can be set only if all its sections are clear and unlocked and all
 *       its points can be moved to the required positions.</li>
 *   <li>Two routes conflict if they share a track section, or if they need the same
 *       point in different positions.</li>
 *   <li>A point cannot move while it is locked or while its section is occupied.</li>
 *   <li>Sections are released one by one as the train passes over them.</li>
 * </ul>
 */
public class InterlockingService {

    private static final Logger LOG = Logger.getLogger(InterlockingService.class.getName());

    private final Map<String, TrackElement> layout = new LinkedHashMap<>();
    private final Map<String, Route> routes = new LinkedHashMap<>();
    private final List<EventLogEntry> events = new ArrayList<>();
    private final StationStore store;

    /**
     * @param layout all sections, points and signals, keyed by id
     * @param routes all routes of the station (objects must come from {@code layout})
     * @param store  where to persist state, or null for in-memory only
     */
    public InterlockingService(Map<String, TrackElement> layout, List<Route> routes, StationStore store) {
        this.store = store;
        replaceState(layout, routes);
    }

    // ------------------------------------------------------------------ route commands

    /**
     * Sets (locks) the given route and clears its entry signal.
     * Must check that every section on the route is clear and unlocked, that the route
     * does not conflict with any active route (shared section or same point needed in a
     * different position), and that each required point can be moved. On success, moves
     * the points, locks all sections and points, and clears the entry signal.
     *
     * @param routeId id of the route to set, e.g. "R1"
     * @throws RouteConflictException    if the route conflicts with an active route
     * @throws PointLockedException      if a required point is locked or cannot move
     * @throws SectionOccupiedException  if any section on the route is occupied
     */
    public void setRoute(String routeId)
            throws RouteConflictException, PointLockedException, SectionOccupiedException {
        Route route = requireRoute(routeId);
        try {
            checkCanSet(route);
        } catch (RouteConflictException | PointLockedException | SectionOccupiedException e) {
            record(routeId, "REFUSED", e.getMessage());
            throw e;
        }

        // All checks passed: move points, lock everything, clear the signal.
        for (Map.Entry<Point, Boolean> e : route.getRequiredPointPositions().entrySet()) {
            Point p = e.getKey();
            p.setReversed(e.getValue()); // cannot fail: checked above
            p.lock();
        }
        for (TrackSection s : route.getSections()) {
            s.lock();
        }
        route.setActive(true);
        route.setTrainPosition(Route.NO_TRAIN);
        route.getEntrySignal().setClear(true);

        persist();
        record(routeId, "SET", route.getPathText() + ", " + route.getEntrySignal().getId() + " GREEN");
    }

    /** Runs every interlocking check for {@link #setRoute} without changing anything. */
    private void checkCanSet(Route route)
            throws RouteConflictException, PointLockedException, SectionOccupiedException {
        if (route.isActive()) {
            throw new RouteConflictException("Route " + route.getId() + " is already set");
        }
        for (TrackSection s : route.getSections()) {
            if (s.isOccupied()) {
                throw new SectionOccupiedException("Cannot set " + route.getId()
                        + ": section " + s.getId() + " is occupied");
            }
        }
        for (Route active : getActiveRoutes()) {
            String conflict = route.conflictWith(active);
            if (conflict != null) {
                throw new RouteConflictException("Cannot set " + route.getId()
                        + ": conflicts with active route " + active.getId() + " (" + conflict + ")");
            }
        }
        for (TrackSection s : route.getSections()) {
            if (s.isLocked()) {
                throw new RouteConflictException("Cannot set " + route.getId()
                        + ": section " + s.getId() + " is locked");
            }
        }
        for (Map.Entry<Point, Boolean> e : route.getRequiredPointPositions().entrySet()) {
            Point p = e.getKey();
            boolean mustMove = p.isReversed() != e.getValue();
            if (mustMove) {
                checkPointCanMove(p);
            }
        }
    }

    /**
     * Cancels an active route: sets the entry signal back to red and releases every
     * section and point still locked by the route. Only allowed while no train occupies it.
     *
     * @param routeId id of the active route to cancel
     * @throws IllegalStateException if the route is not set or a train is on it
     */
    public void cancelRoute(String routeId) {
        Route route = requireRoute(routeId);
        if (!route.isActive()) {
            refuse(routeId, "Route " + routeId + " is not set");
        }
        if (route.hasTrain()) {
            refuse(routeId, "Cannot cancel " + routeId + ": a train is on the route");
        }
        route.getEntrySignal().setClear(false);
        for (TrackSection s : route.getSections()) {
            s.release();
        }
        route.setActive(false);
        releasePoints(route);

        persist();
        record(routeId, "CANCEL", "Route cancelled, " + route.getEntrySignal().getId() + " RED");
    }

    /**
     * Advances the train on the given route by one section per call.
     * The entry signal returns to red as soon as the train enters the route. Each section
     * the train leaves becomes unoccupied and is released; when the train leaves the last
     * section the route is no longer active and its points are released.
     *
     * @param routeId id of the active route the train is travelling on
     * @throws IllegalStateException if the route is not set
     */
    public void moveTrain(String routeId) {
        Route route = requireRoute(routeId);
        if (!route.isActive()) {
            refuse(routeId, "Cannot move train: route " + routeId + " is not set");
        }
        List<TrackSection> sections = route.getSections();
        int pos = route.getTrainPosition();
        String message;

        if (pos == Route.NO_TRAIN) {
            route.getEntrySignal().setClear(false);
            sections.get(0).setOccupied(true);
            route.setTrainPosition(0);
            message = "Train passed " + route.getEntrySignal().getId() + " (now RED), entered "
                    + sections.get(0).getId();
        } else {
            TrackSection left = sections.get(pos);
            left.setOccupied(false);
            left.release();
            if (pos + 1 < sections.size()) {
                TrackSection next = sections.get(pos + 1);
                next.setOccupied(true);
                route.setTrainPosition(pos + 1);
                message = "Train left " + left.getId() + " (released), entered " + next.getId();
            } else {
                route.setTrainPosition(Route.NO_TRAIN);
                route.setActive(false);
                releasePoints(route);
                message = "Train left " + left.getId() + " and the station, route completed";
            }
        }

        persist();
        record(routeId, "MOVE", message);
    }

    // ------------------------------------------------------------------ manual controls

    /**
     * Manually throws a point to its other position (signaller's individual control).
     *
     * @throws PointLockedException if the point is locked by a route or a section at the
     *                              point is occupied
     */
    public void throwPoint(String pointId) throws PointLockedException {
        TrackElement e = layout.get(pointId);
        if (!(e instanceof Point)) {
            throw new IllegalArgumentException("Unknown point: " + pointId);
        }
        Point p = (Point) e;
        try {
            checkPointCanMove(p);
            p.setReversed(!p.isReversed());
        } catch (PointLockedException ex) {
            record("", "REFUSED", ex.getMessage());
            throw ex;
        }
        persist();
        record("", "POINT", p.getId() + " thrown to " + Point.positionName(p.isReversed()));
    }

    /**
     * Simulates a track-circuit report, e.g. a stalled train or a fault. Only allowed on
     * sections that no route has locked (the interlocking owns those).
     *
     * @throws IllegalStateException if the section is locked by a route
     */
    public void setSectionOccupied(String sectionId, boolean occupied) {
        TrackElement e = layout.get(sectionId);
        if (!(e instanceof TrackSection)) {
            throw new IllegalArgumentException("Unknown section: " + sectionId);
        }
        TrackSection s = (TrackSection) e;
        if (s.isLocked()) {
            refuse("", "Section " + sectionId + " is locked by a route; its occupancy is controlled by the train");
        }
        if (s.isOccupied() == occupied) {
            return;
        }
        s.setOccupied(occupied);
        persist();
        record("", "TRACK", sectionId + (occupied ? " reported OCCUPIED" : " reported clear"));
    }

    private void checkPointCanMove(Point p) throws PointLockedException {
        if (p.isLocked()) {
            throw new PointLockedException("Point " + p.getId() + " is locked in "
                    + Point.positionName(p.isReversed()) + " by an active route");
        }
        for (TrackSection s : sectionsAtPoint(p)) {
            if (s.isOccupied()) {
                throw new PointLockedException("Point " + p.getId()
                        + " cannot move: section " + s.getId() + " at the point is occupied");
            }
        }
    }

    /**
     * The section a point lies in: the section shared by every route through the point
     * (P1 lies in T1, P2 in T4). A train standing there is over the point.
     */
    private Set<TrackSection> sectionsAtPoint(Point p) {
        Set<TrackSection> result = null;
        for (Route r : routes.values()) {
            if (r.usesPoint(p)) {
                if (result == null) {
                    result = new HashSet<>(r.getSections());
                } else {
                    result.retainAll(r.getSections());
                }
            }
        }
        return result == null ? new HashSet<>() : result;
    }

    /** Releases a route's points unless another active route still needs them. */
    private void releasePoints(Route finished) {
        for (Point p : finished.getRequiredPointPositions().keySet()) {
            boolean stillNeeded = false;
            for (Route other : routes.values()) {
                if (other != finished && other.isActive() && other.usesPoint(p)) {
                    stillNeeded = true;
                }
            }
            if (!stillNeeded) {
                p.release();
            }
        }
    }

    // ------------------------------------------------------------------ queries

    /**
     * @return the routes that are currently set (locked)
     */
    public List<Route> getActiveRoutes() {
        List<Route> active = new ArrayList<>();
        for (Route r : routes.values()) {
            if (r.isActive()) {
                active.add(r);
            }
        }
        return active;
    }

    /**
     * @return every element of the station layout (sections, points, signals), keyed by id
     */
    public Map<String, TrackElement> getLayout() {
        return Collections.unmodifiableMap(layout);
    }

    /** @return every route of the station, active or not, sorted by id. */
    public List<Route> getRoutes() {
        List<Route> list = new ArrayList<>(routes.values());
        list.sort(Comparator.comparing(Route::getId));
        return list;
    }

    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    /** @return the event log of this session (and any history added with {@link #addHistory}). */
    public List<EventLogEntry> getEventLog() {
        return Collections.unmodifiableList(events);
    }

    /** Prepends older events, e.g. loaded from the database at start-up. */
    public void addHistory(List<EventLogEntry> history) {
        events.addAll(0, history);
    }

    // ------------------------------------------------------------------ serialization

    /** Saves the complete station state (layout + routes) to a file. */
    public void saveSnapshot(File file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            // one stream, so routes keep pointing at the same element objects
            out.writeObject(new LinkedHashMap<>(layout));
            out.writeObject(new ArrayList<>(routes.values()));
        }
        LOG.info("Snapshot saved to " + file.getAbsolutePath());
        record("", "SNAPSHOT", "Saved to " + file.getName());
    }

    /** Replaces the station state with one previously written by {@link #saveSnapshot}. */
    public void loadSnapshot(File file) throws IOException {
        Map<String, TrackElement> newLayout = new LinkedHashMap<>();
        List<Route> newRoutes = new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object layoutObj = in.readObject();
            Object routesObj = in.readObject();
            if (!(layoutObj instanceof Map) || !(routesObj instanceof List)) {
                throw new IOException("Not an interlocking snapshot: " + file.getName());
            }
            for (Map.Entry<?, ?> e : ((Map<?, ?>) layoutObj).entrySet()) {
                if (!(e.getValue() instanceof TrackElement)) {
                    throw new IOException("Corrupt snapshot: bad layout entry " + e.getKey());
                }
                TrackElement el = (TrackElement) e.getValue();
                newLayout.put(el.getId(), el);
            }
            for (Object o : (List<?>) routesObj) {
                if (!(o instanceof Route)) {
                    throw new IOException("Corrupt snapshot: bad route entry");
                }
                newRoutes.add((Route) o);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Snapshot uses unknown classes", e);
        }
        replaceState(newLayout, newRoutes);
        persist();
        LOG.info("Snapshot loaded from " + file.getAbsolutePath());
        record("", "SNAPSHOT", "Loaded from " + file.getName());
    }

    private void replaceState(Map<String, TrackElement> newLayout, List<Route> newRoutes) {
        layout.clear();
        layout.putAll(newLayout);
        routes.clear();
        for (Route r : newRoutes) {
            routes.put(r.getId(), r);
        }
    }

    // ------------------------------------------------------------------ helpers

    private Route requireRoute(String routeId) {
        Route route = routes.get(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Unknown route: " + routeId);
        }
        return route;
    }

    private void refuse(String routeId, String message) {
        record(routeId, "REFUSED", message);
        throw new IllegalStateException(message);
    }

    private void record(String routeId, String action, String message) {
        EventLogEntry entry = new EventLogEntry(LocalDateTime.now(), routeId, action, message);
        events.add(entry);
        LOG.info("[" + action + "] " + entry.getRouteId() + " " + message);
        if (store != null) {
            try {
                store.logEvent(entry);
            } catch (SQLException e) {
                LOG.warning("Could not write event to database: " + e.getMessage());
            }
        }
    }

    private void persist() {
        if (store == null) {
            return;
        }
        try {
            store.saveState(layout.values(), routes.values());
        } catch (SQLException e) {
            LOG.severe("Could not save state to database: " + e.getMessage());
        }
    }
}
