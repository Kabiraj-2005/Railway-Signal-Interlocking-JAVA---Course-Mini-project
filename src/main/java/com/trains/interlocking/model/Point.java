package com.trains.interlocking.model;

import com.trains.interlocking.exception.PointLockedException;

/**
 * A set of movable rails that switches a train between two tracks.
 * Normal leads to the main line, reverse leads to the loop.
 */
public class Point extends TrackElement {

    private static final long serialVersionUID = 1L;

    /** false = normal, true = reverse. */
    private boolean reversed;

    public Point(String id) {
        super(id);
    }

    public boolean isReversed() {
        return reversed;
    }

    /**
     * Moves the point.
     *
     * @throws PointLockedException if the point is locked by a route and would have to move
     */
    public void setReversed(boolean reversed) throws PointLockedException {
        if (this.reversed == reversed) {
            return;
        }
        if (isLocked()) {
            throw new PointLockedException("Point " + getId() + " is locked in "
                    + positionName(this.reversed) + " and cannot move to " + positionName(reversed));
        }
        this.reversed = reversed;
    }

    public static String positionName(boolean reversed) {
        return reversed ? "REVERSE" : "NORMAL";
    }

    @Override
    public String getType() {
        return "Point";
    }

    @Override
    public String getStatus() {
        return positionName(reversed) + ", " + lockText();
    }
}
