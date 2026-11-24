/*
 * Point.java
 * A set of movable rails that sends a train either to the main line (NORMAL)
 * or to the loop (REVERSE). A locked point refuses to move.
 * Lecture decks: Inheritance, Method Overriding, Exception Handling (throws).
 * Owner: Utpal
 */
package com.trains.interlocking.model;

import com.trains.interlocking.exception.PointLockedException;

public class Point extends TrackElement {

    private static final long serialVersionUID = 1L;

    private boolean reversed; // false = NORMAL, true = REVERSE

    public Point(String id) {
        super(id);
    }

    public Point(String id, boolean reversed, boolean locked) {
        super(id, locked);
        this.reversed = reversed;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setReversed(boolean newPosition) throws PointLockedException {
        // Staying in the same position is always safe, even when locked.
        if (newPosition != reversed && isLocked()) {
            throw new PointLockedException("Point " + getId() + " is locked and cannot move");
        }
        reversed = newPosition;
    }

    public static String positionName(boolean reversed) {
        if (reversed) {
            return "REVERSE";
        }
        return "NORMAL";
    }

    @Override
    public String getStatus() {
        if (isLocked()) {
            return positionName(reversed) + ", locked";
        }
        return positionName(reversed);
    }

    @Override
    public String getDisplayColour() {
        if (isLocked()) {
            return "#fdd835"; // yellow
        }
        return "#bdbdbd"; // grey
    }
}
