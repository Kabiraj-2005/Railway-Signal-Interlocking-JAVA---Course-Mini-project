package com.trains.interlocking.exception;

/**
 * Thrown when a requested route conflicts with an already active route.
 */
public class RouteConflictException extends Exception {

    private static final long serialVersionUID = 1L;

    public RouteConflictException(String message) {
        super(message);
    }
}
