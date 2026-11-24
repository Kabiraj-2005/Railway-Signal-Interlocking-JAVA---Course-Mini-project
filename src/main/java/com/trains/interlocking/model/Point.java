package com.trains.interlocking.model;

/*
 * TODO (Utpal): Point — a set of movable rails that switches between two tracks.
 *  - private boolean reversed (false = normal, true = reverse), with isReversed().
 *  - setReversed(boolean) throws PointLockedException if the point is locked.
 *  - getStatus(): describe NORMAL/REVERSE and locked/unlocked.
 */
public class Point extends TrackElement {

    public Point(String id) {
        super(id);
    }

    @Override
    public String getStatus() {
        return "TODO";
    }
}
