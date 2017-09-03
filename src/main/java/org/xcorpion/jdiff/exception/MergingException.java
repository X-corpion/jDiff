package org.xcorpion.jdiff.exception;

public class MergingException extends RuntimeException {

    public MergingException(String message) {
        super(message);
    }

    public MergingException(String message, Throwable cause) {
        super(message, cause);
    }

}
