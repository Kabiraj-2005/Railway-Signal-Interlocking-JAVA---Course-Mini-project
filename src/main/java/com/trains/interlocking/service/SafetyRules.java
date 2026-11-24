/*
 * SafetyRules.java
 * The interlocking safety checks, kept in one place. These methods only CHECK:
 * they never change anything. If a rule is broken they throw a checked exception
 * that explains why; if they return normally, the action is safe.
 * Lecture decks: static methods, Collections (ArrayList, HashSet.retainAll), Exception Handling.
 * Owner: Utpal
 */
package com.trains.interlocking.service;

import com.trains.interlocking.exception.PointLockedException;
import com.trains.interlocking.exception.RouteConflictException;
import com.trains.interlocking.exception.SectionOccupiedException;
import com.trains.interlocking.model.Point;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackSection;

import java.util.ArrayList;
import java.util.HashSet;

public class SafetyRules {

    /** Rules 1, 2 and 3: may this route be set right now? */
    public static void checkRouteCanBeSet(Route route, ArrayList<Route> allRoutes)
            throws RouteConflictException, PointLockedException, SectionOccupiedException {
        if (route.isActive()) {
            throw new RouteConflictException("Route " + route.getId() + " is already set");
        }
        // Rule 1: every section must be free...
        for (TrackSection section : route.getSections()) {
            if (section.isOccupied()) {
                throw new SectionOccupiedException("Section " + section.getId() + " is occupied");
            }
        }
        // Rule 2: no conflict with a route that is already set...
        for (Route other : allRoutes) {
            if (other.isActive()) {
                String reason = route.conflictWith(other);
                if (reason != null) {
                    throw new RouteConflictException("Conflicts with " + other.getId() + ": " + reason);
                }
            }
        }
        // ...and every section must be unlocked.
        for (TrackSection section : route.getSections()) {
            if (section.isLocked()) {
                throw new RouteConflictException("Section " + section.getId() + " is locked");
            }
        }
        // Rule 3: every point that has to move must be able to move.
        for (Point point : route.getPointPositions().keySet()) {
            if (point.isReversed() != route.getPointPositions().get(point)) {
                checkPointCanMove(point, allRoutes);
            }
        }
    }

    /** Rule 3: a point cannot move while it is locked or a train is over it. */
    public static void checkPointCanMove(Point point, ArrayList<Route> allRoutes)
            throws PointLockedException {
        if (point.isLocked()) {
            throw new PointLockedException("Point " + point.getId() + " is locked by a route");
        }
        for (TrackSection section : sectionsAtPoint(point, allRoutes)) {
            if (section.isOccupied()) {
                throw new PointLockedException("Point " + point.getId()
                        + " has a train over it (section " + section.getId() + ")");
            }
        }
    }

    /**
     * The section a point lies in is the one shared by ALL routes through it
     * (P1 is in T1, P2 is in T4). retainAll keeps only the sections in common.
     */
    private static HashSet<TrackSection> sectionsAtPoint(Point point, ArrayList<Route> allRoutes) {
        HashSet<TrackSection> common = null;
        for (Route route : allRoutes) {
            if (route.usesPoint(point)) {
                if (common == null) {
                    common = new HashSet<>(route.getSections());
                } else {
                    common.retainAll(route.getSections());
                }
            }
        }
        if (common == null) {
            return new HashSet<>(); // a point that no route uses
        }
        return common;
    }
}
