package com.trains.interlocking.model;

/*
 * TODO (Utpal): TrackSection — a block of track that a train can occupy.
 *  - private boolean occupied, with isOccupied() / setOccupied(boolean).
 *  - getStatus(): describe occupied/clear and locked/unlocked.
 */
public class TrackSection extends TrackElement {

    public TrackSection(String id) {
        super(id);
    }

    @Override
    public String getStatus() {
        return "TODO";
    }
}
