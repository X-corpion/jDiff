package org.xcorpion.jdiff.api;

public class Diff {

    public enum Operation {

        NO_OP,
        ADD_VALUE,
        UPDATE_VALUE,
        REMOVE_VALUE,
        RESIZE_ARRAY

    }

    protected Operation operation;
    protected Object srcValue;
    protected Object targetValue;

    private Diff() {
    }

    public Diff(Operation operation, Object srcValue, Object targetValue) {
        this();
        this.operation = operation;
        this.srcValue = srcValue;
        this.targetValue = targetValue;
    }

    public Operation getOperation() {
        return operation;
    }

    public Object getSrcValue() {
        return srcValue;
    }

    public Object getTargetValue() {
        return targetValue;
    }

    @Override
    public String toString() {
        return operation + ": " + srcValue + " -> " + targetValue;
    }
}
