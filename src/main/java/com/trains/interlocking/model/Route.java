/*
 * Route.java
 * A path a train can take through the station: an entry signal, the sections in the
 * order the train runs over them, and the position each point must be in.
 * It also remembers whether it is set (active) and which section the train is in.
 * Lecture decks: Classes and Objects, Collections (ArrayList, HashMap), Serialization.
 * Owner: Utpal
 */
package com.trains.interlocking.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Route implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Train position meaning "no train on the route yet". */
    public static final int NO_TRAIN = -1;

    private final String id;
    private final String description;
    private final Signal entrySignal;
    private final ArrayList<TrackSection> sections;
    private final HashMap<Point, Boolean> pointPositions; // true = REVERSE

    private boolean active;
    private int trainPosition = NO_TRAIN; // index into sections

    public Route(String id, String description, Signal entrySignal,
                 ArrayList<TrackSection> sections, HashMap<Point, Boolean> pointPositions) {
        this.id = id;
        this.description = description;
        this.entrySignal = entrySignal;
        this.sections = sections;
        this.pointPositions = pointPositions;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Signal getEntrySignal() {
        return entrySignal;
    }

    // The lists are returned directly: a route never changes after it is built.
    public ArrayList<TrackSection> getSections() {
        return sections;
    }

    public HashMap<Point, Boolean> getPointPositions() {
        return pointPositions;
    }

    public boolean usesPoint(Point point) {
        return pointPositions.containsKey(point);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getTrainPosition() {
        return trainPosition;
    }

    public void setTrainPosition(int trainPosition) {
        this.trainPosition = trainPosition;
    }

    public boolean hasTrain() {
        return trainPosition != NO_TRAIN;
    }

    /**
     * Two routes conflict if they share a section, or need the same point in
     * different positions. Returns the reason, or null if they can both be set.
     */
    public String conflictWith(Route other) {
        for (TrackSection section : sections) {
            if (other.getSections().contains(section)) {
                return "both use section " + section.getId();
            }
        }
        for (Point point : pointPositions.keySet()) {
            if (other.usesPoint(point)) {
                boolean mine = pointPositions.get(point);
                boolean theirs = other.getPointPositions().get(point);
                if (mine != theirs) {
                    return "they need point " + point.getId() + " in different positions";
                }
            }
        }
        return null;
    }

    /** e.g. "T1 -> T2, P1 NORMAL" */
    public String getPathText() {
        String text = "";
        for (TrackSection section : sections) {
            if (!text.isEmpty()) {
                text = text + " -> ";
            }
            text = text + section.getId();
        }
        for (Point point : pointPositions.keySet()) {
            text = text + ", " + point.getId() + " " + Point.positionName(pointPositions.get(point));
        }
        return text;
    }

    public String getStateText() {
        if (!active) {
            return "not set";
        } else if (!hasTrain()) {
            return "SET, train waiting at " + entrySignal.getId();
        } else {
            return "SET, train in " + sections.get(trainPosition).getId();
        }
    }

    @Override
    public String toString() {
        return id + "  " + description + "  (" + getPathText() + ")  " + getStateText();
    }
}
