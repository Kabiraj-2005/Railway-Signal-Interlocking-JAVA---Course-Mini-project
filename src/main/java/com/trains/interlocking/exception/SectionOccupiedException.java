package com.trains.interlocking.exception;

/**
 * Thrown when a track section needed by a route is occupied.
 */
public class SectionOccupiedException extends Exception {

    public SectionOccupiedException(String message) {
        super(message);
    }
}
