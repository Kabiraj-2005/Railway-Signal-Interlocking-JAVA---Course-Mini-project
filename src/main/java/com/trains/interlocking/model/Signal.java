/*
 * Signal.java
 * A signal at the entrance of a route: RED means stop, GREEN means the route
 * is set and the train may enter.
 * Lecture decks: Inheritance, Method Overriding.
 * Owner: Utpal
 */
package com.trains.interlocking.model;

public class Signal extends TrackElement {

    private static final long serialVersionUID = 1L;

    private boolean clear; // false = RED, true = GREEN

    public Signal(String id) {
        super(id);
    }

    public Signal(String id, boolean clear) {
        super(id);
        this.clear = clear;
    }

    public boolean isClear() {
        return clear;
    }

    public void setClear(boolean clear) {
        this.clear = clear;
    }

    @Override
    public String getStatus() {
        if (clear) {
            return "GREEN";
        }
        return "RED";
    }

    @Override
    public String getDisplayColour() {
        if (clear) {
            return "#43a047"; // green
        }
        return "#e53935"; // red
    }
}
