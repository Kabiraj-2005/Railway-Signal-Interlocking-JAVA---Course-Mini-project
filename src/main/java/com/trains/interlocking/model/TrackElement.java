package com.trains.interlocking.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Base class for every element of the station layout (sections, points, signals).
 * An element has a fixed id and can be locked by the interlocking while a route uses it.
 */
public abstract class TrackElement implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private boolean locked;

    protected TrackElement(String id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
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

    /** Human-readable state of this element, e.g. for display in the UI. */
    public abstract String getStatus();
}
