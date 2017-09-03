package org.xcorpion.jdiff.api;

import java.util.HashMap;
import java.util.Map;

public class DiffNode {

    protected Diff diff;
    protected Map<Object, DiffNode> fieldDiffs;

    public DiffNode() {
        diff = new Diff();
    }

    public DiffNode(Diff diff) {
        this.diff = diff;
    }

    public DiffNode(Diff diff, Map<Object, DiffNode> fieldDiffs) {
        this.diff = diff;
        this.fieldDiffs = fieldDiffs;
    }

    public Diff getDiff() {
        return diff;
    }

    public boolean isEmpty() {
        if (diff != null && diff.getOperation() != null &&
                diff.getOperation() != Diff.Operation.NO_OP) {
            return false;
        }
        if (fieldDiffs != null && !fieldDiffs.isEmpty()) {
            return false;
        }
        return true;
    }

    public Map<Object, DiffNode> getFieldDiffs() {
        return fieldDiffs;
    }

    public void addFieldDiff(Object field, DiffNode diffNode) {
        if (this.fieldDiffs == null) {
            this.fieldDiffs = new HashMap<>();
        }
        this.fieldDiffs.put(field, diffNode);
    }
}
