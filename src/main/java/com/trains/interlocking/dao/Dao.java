package com.trains.interlocking.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object. Harsh writes the implementations.
 *
 * @param <T> the entity type stored by this DAO
 */
public interface Dao<T> {

    void save(T t) throws SQLException;

    Optional<T> findById(String id) throws SQLException;

    List<T> findAll() throws SQLException;

    void update(T t) throws SQLException;

    void delete(String id) throws SQLException;
}
