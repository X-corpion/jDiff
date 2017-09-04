package org.xcorpion.jdiff.internal.model;

import org.xcorpion.jdiff.api.DiffingContext;
import org.xcorpion.jdiff.api.ObjectDiffMapper;

public class DefaultDiffingContext implements DiffingContext {

    private ObjectDiffMapper objectDiffMapper;

    public DefaultDiffingContext(ObjectDiffMapper objectDiffMapper) {
        this.objectDiffMapper = objectDiffMapper;
    }

    @Override
    public ObjectDiffMapper getObjectDiffMapper() {
        return objectDiffMapper;
    }
}
