/*
 * PointLockedException.java
 * A custom CHECKED exception: it extends Exception (not RuntimeException), so the
 * compiler forces every caller to catch it or declare it with "throws".
 * Thrown when a point cannot move: it is locked by a route or a train is over it.
 * Lecture deck: Exception Handling (custom checked exceptions, throw/throws).
 * Owner: Utpal
 */
package com.trains.interlocking.exception;

public class PointLockedException extends Exception {

    // Exception is Serializable, so it should have a version number.
    private static final long serialVersionUID = 1L;

    public PointLockedException(String message) {
        super(message);
    }
}
