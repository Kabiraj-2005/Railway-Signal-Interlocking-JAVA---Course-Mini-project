package com.trains.interlocking.exception;

/**
 * Thrown when a track section needed by a route is occupied.
 */
public class SectionOccupiedException extends Exception {

    private static final long serialVersionUID = 1L;

    public SectionOccupiedException(String message) {
        super(message);
    }
}
