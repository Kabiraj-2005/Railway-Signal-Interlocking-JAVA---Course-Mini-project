/*
 * TrackElement.java
 * The abstract parent class of every piece of the station: sections, points and signals.
 * It holds what they all share (an id and a locked flag) and forces each child class
 * to describe itself through two abstract methods.
 * Lecture decks: Inheritance, Abstract Classes, Serialization.
 * Owner: Utpal
 *
 * Note: we do NOT override equals()/hashCode(). The service, the DAOs and the snapshot
 * loader all reuse the SAME objects from the one layout map, so Java's default
 * "same object" comparison is exactly what we want.
 */
package com.trains.interlocking.model;

import java.io.Serializable;
import java.util.Objects;

public abstract class TrackElement implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private boolean locked;

    protected TrackElement(String id) {
        this(id, false);
    }

    // Used by the DAOs, which rebuild elements exactly as they were stored.
    protected TrackElement(String id, boolean locked) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.locked = locked;
    }

    public String getId() {
        return id;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        locked = true;
    }

    public void release() {
        locked = false;
    }

    /** Short state text for the screen, e.g. "occupied" or "NORMAL, locked". */
    public abstract String getStatus();

    /** Colour the UI paints this element in (each child class decides). */
    public abstract String getDisplayColour();
}
