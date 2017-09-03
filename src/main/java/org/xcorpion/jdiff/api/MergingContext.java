package org.xcorpion.jdiff.api;

import java.util.Set;
import javax.annotation.Nonnull;

public interface MergingContext {

    @Nonnull
    ObjectDiffMapper getObjectDiffMapper();

    @Nonnull
    Set<Feature.MergingStrategy> getMergingStrategies();

    boolean isRootObject();

}
