package com.trains.interlocking.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A path a train can be given through the station: an entry signal, the sections in the
 * order the train traverses them, and the position each point on the path must be in.
 *
 * <p>A route also carries its run-time state: whether it is set (active) and where the
 * train is. {@code trainPosition} is -1 while the train waits at the entry signal and
 * otherwise the index of the section the train currently occupies.
 */
public class Route implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int NO_TRAIN = -1;

    private final String id;
    private final String description;
    private final Signal entrySignal;
    private final ArrayList<TrackSection> sections;
    /** true = reverse, false = normal. */
    private final HashMap<Point, Boolean> requiredPointPositions;

    private boolean active;
    private int trainPosition = NO_TRAIN;

    public Route(String id, String description, Signal entrySignal,
                 List<TrackSection> sections, Map<Point, Boolean> requiredPointPositions) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.description = description == null ? "" : description;
        this.entrySignal = Objects.requireNonNull(entrySignal, "entrySignal must not be null");
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Route " + id + " needs at least one section");
        }
        this.sections = new ArrayList<>(sections);
        this.requiredPointPositions = requiredPointPositions == null
                ? new HashMap<>() : new HashMap<>(requiredPointPositions);
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

    public List<TrackSection> getSections() {
        return new ArrayList<>(sections);
    }

    public Map<Point, Boolean> getRequiredPointPositions() {
        return new HashMap<>(requiredPointPositions);
    }

    public boolean usesSection(TrackSection section) {
        return sections.contains(section);
    }

    public boolean usesPoint(Point point) {
        return requiredPointPositions.containsKey(point);
    }

    /** @return the required position of the point (true = reverse), or null if not used. */
    public Boolean requiredPosition(Point point) {
        return requiredPointPositions.get(point);
    }

    /**
     * Two routes conflict if they share a track section or need the same point in
     * different positions.
     *
     * @return a description of the conflict, or null if the routes are compatible
     */
    public String conflictWith(Route other) {
        for (TrackSection s : sections) {
            if (other.usesSection(s)) {
                return "both use section " + s.getId();
            }
        }
        for (Map.Entry<Point, Boolean> e : requiredPointPositions.entrySet()) {
            Boolean otherPos = other.requiredPosition(e.getKey());
            if (otherPos != null && !otherPos.equals(e.getValue())) {
                return "point " + e.getKey().getId() + " is needed "
                        + Point.positionName(e.getValue()) + " but " + other.getId()
                        + " needs it " + Point.positionName(otherPos);
            }
        }
        return null;
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
        if (trainPosition < NO_TRAIN || trainPosition >= sections.size()) {
            throw new IllegalArgumentException("Invalid train position " + trainPosition);
        }
        this.trainPosition = trainPosition;
    }

    public boolean hasTrain() {
        return trainPosition != NO_TRAIN;
    }

    /** One-line text such as "T1 -> T2 (P1 NORMAL)". */
    public String getPathText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) {
                sb.append(" -> ");
            }
            sb.append(sections.get(i).getId());
        }
        if (!requiredPointPositions.isEmpty()) {
            sb.append(" (");
            boolean first = true;
            for (Map.Entry<Point, Boolean> e : requiredPointPositions.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(e.getKey().getId()).append(' ').append(Point.positionName(e.getValue()));
                first = false;
            }
            sb.append(')');
        }
        return sb.toString();
    }

    /** Run-time state for display. */
    public String getStateText() {
        if (!active) {
            return "idle";
        }
        if (!hasTrain()) {
            return "set, train waiting at " + entrySignal.getId();
        }
        return "train in " + sections.get(trainPosition).getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Route)) {
            return false;
        }
        return id.equals(((Route) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " - " + description;
    }
}
