package com.trains.interlocking.model;

/**
 * A block of track that a train can occupy. Occupancy is what a track circuit reports.
 */
public class TrackSection extends TrackElement {

    private static final long serialVersionUID = 1L;

    private final String description;
    private boolean occupied;

    public TrackSection(String id) {
        this(id, "");
    }

    public TrackSection(String id, String description) {
        super(id);
        this.description = description == null ? "" : description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    @Override
    public String getType() {
        return "Section";
    }

    @Override
    public String getStatus() {
        return (occupied ? "OCCUPIED" : "clear") + ", " + lockText();
    }
}
