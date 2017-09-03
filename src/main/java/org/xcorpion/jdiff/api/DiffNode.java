package org.xcorpion.jdiff.api;

import java.util.HashMap;
import java.util.Map;

public class DiffNode {

    protected Diff diff;
    protected Map<Object, DiffNode> fieldDiffs;

    public DiffNode() {
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
