package org.xcorpion.jdiff.exception;

public class DiffApplicationException extends RuntimeException {

    public DiffApplicationException() {
    }

    public DiffApplicationException(String message) {
        super(message);
    }

    public DiffApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
