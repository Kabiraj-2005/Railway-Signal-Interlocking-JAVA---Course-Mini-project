package com.trains.interlocking.service;

import com.trains.interlocking.model.EventLogEntry;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackElement;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Where the interlocking persists its state. Implemented by the database layer
 * ({@code dao.StationRepository}); the service also works without one (in-memory only).
 */
public interface StationStore {

    /** Saves the current state of every element and route. */
    void saveState(Collection<TrackElement> elements, Collection<Route> routes) throws SQLException;

    /** Appends one entry to the persistent event log. */
    void logEvent(EventLogEntry entry) throws SQLException;
}
