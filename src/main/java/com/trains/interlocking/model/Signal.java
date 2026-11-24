package com.trains.interlocking.model;

/**
 * Controls entry to a route. Red (stop) by default; green only while a route is set
 * and the train has not yet entered it.
 */
public class Signal extends TrackElement {

    private static final long serialVersionUID = 1L;

    /** false = red, true = green. */
    private boolean clear;

    public Signal(String id) {
        super(id);
    }

    public boolean isClear() {
        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }

    @Override
    public String getType() {
        return "Signal";
    }

    @Override
    public String getStatus() {
        return clear ? "GREEN" : "RED";
    }
}
