package org.xcorpion.jdiff.internal.model;

import java.util.Set;

import javax.annotation.Nonnull;

import org.xcorpion.jdiff.api.Feature;
import org.xcorpion.jdiff.api.MergingContext;
import org.xcorpion.jdiff.api.ObjectDiffMapper;

public class DefaultMergingContext implements MergingContext {

    private ObjectDiffMapper objectDiffMapper;
    private Set<Feature.MergingStrategy> mergingStrategies;
    private boolean isRootObject;

    public DefaultMergingContext(
            ObjectDiffMapper objectDiffMapper,
            Set<Feature.MergingStrategy> mergingStrategies,
            boolean isRootObject
    ) {
        this.objectDiffMapper = objectDiffMapper;
        this.mergingStrategies = mergingStrategies;
        this.isRootObject = isRootObject;
    }

    @Nonnull
    @Override
    public ObjectDiffMapper getObjectDiffMapper() {
        return objectDiffMapper;
    }

    @Nonnull
    @Override
    public Set<Feature.MergingStrategy> getMergingStrategies() {
        return mergingStrategies;
    }

    @Override
    public boolean isRootObject() {
        return isRootObject;
    }
}
