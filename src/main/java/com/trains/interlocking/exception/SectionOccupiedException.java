/*
 * SectionOccupiedException.java
 * A custom CHECKED exception: it extends Exception (not RuntimeException), so the
 * compiler forces every caller to catch it or declare it with "throws".
 * Thrown when a track section needed for a request is occupied or locked.
 * Lecture deck: Exception Handling (custom checked exceptions, throw/throws).
 * Owner: Utpal
 */
package com.trains.interlocking.exception;

public class SectionOccupiedException extends Exception {

    // Exception is Serializable, so it should have a version number.
    private static final long serialVersionUID = 1L;

    public SectionOccupiedException(String message) {
        super(message);
    }
}
