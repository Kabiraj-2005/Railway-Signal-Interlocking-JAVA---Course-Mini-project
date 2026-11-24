/*
 * Dao.java
 * The generic DAO (Data Access Object) interface. Each DAO class implements it for
 * one kind of object, e.g. TrackSectionDao implements Dao<TrackSection>.
 * T is a type parameter: it is replaced by the real class in each implementation.
 * Lecture decks: Generics, Interfaces, DAO pattern, Optional.
 * Owner: Harsh
 */
package com.trains.interlocking.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface Dao<T> {

    /** Every row of the table, as objects. */
    List<T> findAll() throws SQLException;

    /** One object, or Optional.empty() if no row has this id. */
    Optional<T> findById(String id) throws SQLException;

    /** Writes the object's current state back to its row. */
    void update(T item) throws SQLException;
}
