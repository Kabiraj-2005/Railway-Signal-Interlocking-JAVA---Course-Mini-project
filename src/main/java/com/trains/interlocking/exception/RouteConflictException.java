package com.trains.interlocking.exception;

/**
 * Thrown when a requested route conflicts with an already active route.
 */
public class RouteConflictException extends Exception {

    public RouteConflictException(String message) {
        super(message);
    }
}
