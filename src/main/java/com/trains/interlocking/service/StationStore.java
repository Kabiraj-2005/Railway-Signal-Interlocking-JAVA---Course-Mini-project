/*
 * StationStore.java
 * An interface describing "somewhere the station can be saved". The service only
 * knows this interface; the database class StationRepository implements it.
 * This keeps the interlocking rules separate from SQL.
 * Lecture decks: Interfaces, JDBC (SQLException).
 * Owner: Utpal
 */
package com.trains.interlocking.service;

import com.trains.interlocking.model.EventLogEntry;
import com.trains.interlocking.model.Route;
import com.trains.interlocking.model.TrackElement;

import java.sql.SQLException;
import java.util.List;

public interface StationStore {

    void saveState(List<TrackElement> elements, List<Route> routes) throws SQLException;

    void logEvent(EventLogEntry entry) throws SQLException;
}
