/*
 * InterlockingService.java
 * The "brain" of the project. It carries out every action (set, cancel, move train,
 * manual controls) after SafetyRules has checked it, records each action in the event
 * log, and saves the state through a StationStore. The GUI and tests only call this class.
 * Lecture decks: Collections, Exception Handling, Logging.
 * Owner: Utpal
 */
package com.trains.interlocking.service;

import com.trains.interlocking.exception.PointLockedException;
import com.trains.interlocking.exception.RouteConflictException;
import com.trains.interlocking.exception.SectionOccupiedException;
import com.trains.interlocking.model.EventLogEntry;
import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackElement;
import com.trains.interlocking.model.TrackSection;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class InterlockingService {

    private static final Logger LOG = Logger.getLogger(InterlockingService.class.getName());

    private HashMap<String, TrackElement> layout;
    private ArrayList<Route> routes;
    private final ArrayList<EventLogEntry> events = new ArrayList<>();
    private final StationStore store; // null = keep everything in memory only

    public InterlockingService(HashMap<String, TrackElement> layout, ArrayList<Route> routes,
                               StationStore store) {
        this.layout = layout;
        this.routes = routes;
        this.store = store;
    }

    public void setRoute(String routeId)
            throws RouteConflictException, PointLockedException, SectionOccupiedException {
        Route route = findRoute(routeId);
        try {
            SafetyRules.checkRouteCanBeSet(route, routes);
        } catch (RouteConflictException e) {
            // Log the refusal, then pass the same exception on to the caller.
            addToLog(routeId, "REFUSED", e.getMessage());
            throw e;
        } catch (PointLockedException e) {
            addToLog(routeId, "REFUSED", e.getMessage());
            throw e;
        } catch (SectionOccupiedException e) {
            addToLog(routeId, "REFUSED", e.getMessage());
            throw e;
        }
        // Every check passed, so it is now safe to move the points and lock everything.
        for (Point point : route.getPointPositions().keySet()) {
            point.setReversed(route.getPointPositions().get(point));
            point.lock();
        }
        for (TrackSection section : route.getSections()) {
            section.lock();
        }
        route.setActive(true);
        route.setTrainPosition(Route.NO_TRAIN);
        route.getEntrySignal().setClear(true);
        saveAndRecord(routeId, "SET", route.getPathText() + ", signal GREEN");
    }

    public void cancelRoute(String routeId) throws RouteConflictException {
        Route route = findRoute(routeId);
        if (!route.isActive()) {
            refuse(routeId, "Route " + routeId + " is not set");
        }
        if (route.hasTrain()) {
            // Once the train has passed the signal, the driver relies on the route.
            refuse(routeId, "A train is on route " + routeId + ", it cannot be cancelled");
        }
        route.getEntrySignal().setClear(false);
        for (TrackSection section : route.getSections()) {
            section.release();
        }
        route.setActive(false);
        releasePoints(route);
        saveAndRecord(routeId, "CANCEL", "Route cancelled, signal RED");
    }

    /** Moves the train one section forward. One call = one click in the GUI. */
    public void moveTrain(String routeId) throws RouteConflictException {
        Route route = findRoute(routeId);
        if (!route.isActive()) {
            refuse(routeId, "Route " + routeId + " is not set");
        }
        ArrayList<TrackSection> sections = route.getSections();
        int position = route.getTrainPosition();

        if (position == Route.NO_TRAIN) {
            // The train enters: the signal goes back to RED behind it.
            route.getEntrySignal().setClear(false);
            sections.get(0).setOccupied(true);
            route.setTrainPosition(0);
            saveAndRecord(routeId, "MOVE", "Train entered " + sections.get(0).getId() + ", signal RED");
            return;
        }
        // The train leaves its section, which is freed straight away for other routes.
        TrackSection leaving = sections.get(position);
        leaving.setOccupied(false);
        leaving.release();
        if (position + 1 < sections.size()) {
            TrackSection next = sections.get(position + 1);
            next.setOccupied(true);
            route.setTrainPosition(position + 1);
            saveAndRecord(routeId, "MOVE", "Train left " + leaving.getId() + ", entered " + next.getId());
        } else {
            route.setTrainPosition(Route.NO_TRAIN);
            route.setActive(false);
            releasePoints(route);
            saveAndRecord(routeId, "MOVE", "Train left the station, route finished");
        }
    }

    public void throwPoint(String pointId) throws PointLockedException {
        Point point = (Point) layout.get(pointId);
        try {
            SafetyRules.checkPointCanMove(point, routes);
            point.setReversed(!point.isReversed());
        } catch (PointLockedException e) {
            addToLog("", "REFUSED", e.getMessage());
            throw e;
        }
        saveAndRecord("", "POINT", pointId + " moved to " + Point.positionName(point.isReversed()));
    }

    /** Pretends a track circuit reported a train (or a fault) on a section. */
    public void setSectionOccupied(String sectionId, boolean occupied) throws SectionOccupiedException {
        TrackSection section = (TrackSection) layout.get(sectionId);
        if (section.isLocked()) {
            // A locked section belongs to a route; only the train may change it.
            String message = "Section " + sectionId + " is locked by a route";
            addToLog("", "REFUSED", message);
            throw new SectionOccupiedException(message);
        }
        section.setOccupied(occupied);
        saveAndRecord("", "TRACK", sectionId + (occupied ? " marked occupied" : " marked clear"));
    }

    /**
     * Unlocks a route's points. No other active route can need them: the two routes
     * through a point always share its section, so they can never both be set.
     */
    private void releasePoints(Route finished) {
        for (Point point : finished.getPointPositions().keySet()) {
            point.release();
        }
    }

    public ArrayList<Route> getActiveRoutes() {
        ArrayList<Route> active = new ArrayList<>();
        for (Route route : routes) {
            if (route.isActive()) {
                active.add(route);
            }
        }
        return active;
    }

    // Copies are returned so that callers cannot add or remove items from our lists.
    public ArrayList<Route> getRoutes() {
        return new ArrayList<>(routes);
    }

    public HashMap<String, TrackElement> getLayout() {
        return new HashMap<>(layout);
    }

    public ArrayList<EventLogEntry> getEventLog() {
        return new ArrayList<>(events);
    }

    /** Returns the route with this id, or null if there is none. */
    public Route getRoute(String routeId) {
        for (Route route : routes) {
            if (route.getId().equals(routeId)) {
                return route;
            }
        }
        return null;
    }

    /** Adds older events (loaded from the database) to the front of the log. */
    public void addHistory(List<EventLogEntry> history) {
        events.addAll(0, history);
    }

    public void saveSnapshot(String fileName) throws IOException {
        SnapshotFile.save(fileName, layout, routes);
        addToLog("", "SNAPSHOT", "Saved to " + fileName);
    }

    public void loadSnapshot(String fileName) throws IOException, ClassNotFoundException {
        HashMap<String, TrackElement> newLayout = new HashMap<>();
        ArrayList<Route> newRoutes = new ArrayList<>();
        SnapshotFile.load(fileName, newLayout, newRoutes);
        // Only replace the state once the whole file was read without errors.
        layout = newLayout;
        routes = newRoutes;
        saveAndRecord("", "SNAPSHOT", "Loaded from " + fileName);
    }

    private Route findRoute(String routeId) throws RouteConflictException {
        Route route = getRoute(routeId);
        if (route == null) {
            throw new RouteConflictException("There is no route called " + routeId);
        }
        return route;
    }

    private void refuse(String routeId, String message) throws RouteConflictException {
        addToLog(routeId, "REFUSED", message);
        throw new RouteConflictException(message);
    }

    private void saveAndRecord(String routeId, String action, String message) {
        if (store != null) {
            try {
                store.saveState(new ArrayList<>(layout.values()), routes);
            } catch (SQLException e) {
                // The interlocking keeps working in memory even if the database fails.
                LOG.severe("Could not save state: " + e.getMessage());
            }
        }
        addToLog(routeId, action, message);
    }

    private void addToLog(String routeId, String action, String message) {
        EventLogEntry entry = new EventLogEntry(LocalDateTime.now().withNano(0), routeId, action, message);
        events.add(entry);
        LOG.info(action + " " + routeId + " " + message);
        if (store != null) {
            try {
                store.logEvent(entry);
            } catch (SQLException e) {
                LOG.warning("Could not write event: " + e.getMessage());
            }
        }
    }
}
