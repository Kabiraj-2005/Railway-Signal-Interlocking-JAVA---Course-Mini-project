/*
 * RouteConflictException.java
 * A custom CHECKED exception: it extends Exception (not RuntimeException), so the
 * compiler forces every caller to catch it or declare it with "throws".
 * Thrown when a route request clashes with an active route (or is not allowed now).
 * Lecture deck: Exception Handling (custom checked exceptions, throw/throws).
 * Owner: Utpal
 */
package com.trains.interlocking.exception;

public class RouteConflictException extends Exception {

    // Exception is Serializable, so it should have a version number.
    private static final long serialVersionUID = 1L;

    public RouteConflictException(String message) {
        super(message);
    }
}
