package com.trains.interlocking.exception;

/**
 * Thrown when a point cannot be moved because it is locked or its section is occupied.
 */
public class PointLockedException extends Exception {

    public PointLockedException(String message) {
        super(message);
    }
}
