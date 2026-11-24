/*
 * TrackSection.java
 * A block of track that a train can occupy. In a real railway a "track circuit"
 * detects the train; here the occupied flag plays that role.
 * Lecture decks: Inheritance (extends, super), Method Overriding.
 * Owner: Utpal
 */
package com.trains.interlocking.model;

public class TrackSection extends TrackElement {

    private static final long serialVersionUID = 1L;

    private boolean occupied;

    public TrackSection(String id) {
        super(id);
    }

    public TrackSection(String id, boolean occupied, boolean locked) {
        super(id, locked);
        this.occupied = occupied;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    @Override
    public String getStatus() {
        if (occupied) {
            return "occupied";
        } else if (isLocked()) {
            return "locked";
        } else {
            return "free";
        }
    }

    @Override
    public String getDisplayColour() {
        if (occupied) {
            return "#e53935"; // red
        } else if (isLocked()) {
            return "#fdd835"; // yellow
        } else {
            return "#bdbdbd"; // grey
        }
    }
}
