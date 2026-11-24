package com.trains.interlocking.model;

/*
 * TODO (Utpal): Signal — controls entry to a route.
 *  - private boolean clear (false = red, true = green), with isClear() / setClear(boolean).
 *  - getStatus(): describe RED/GREEN.
 */
public class Signal extends TrackElement {

    public Signal(String id) {
        super(id);
    }

    @Override
    public String getStatus() {
        return "TODO";
    }
}
