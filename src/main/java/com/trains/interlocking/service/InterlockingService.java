package com.trains.interlocking.service;

import com.trains.interlocking.exception.PointLockedException;
import com.trains.interlocking.exception.RouteConflictException;
import com.trains.interlocking.exception.SectionOccupiedException;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackElement;

import java.util.List;
import java.util.Map;

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
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Cancels an active route: sets the entry signal back to red and releases every
     * section and point still locked by the route. Only allowed while no train occupies it.
     *
     * @param routeId id of the active route to cancel
     */
    public void cancelRoute(String routeId) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Advances the train on the given route by one section per call.
     * The entry signal returns to red as soon as the train enters the route. Each section
     * the train leaves becomes unoccupied and is released; when the train leaves the last
     * section the route is no longer active and its points are released.
     *
     * @param routeId id of the active route the train is travelling on
     */
    public void moveTrain(String routeId) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @return the routes that are currently set (locked)
     */
    public List<Route> getActiveRoutes() {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * @return every element of the station layout (sections, points, signals), keyed by id
     */
    public Map<String, TrackElement> getLayout() {
        throw new UnsupportedOperationException("TODO");
    }
}
