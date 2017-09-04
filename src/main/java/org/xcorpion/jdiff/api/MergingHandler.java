package org.xcorpion.jdiff.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.xcorpion.jdiff.exception.MergingException;
import org.xcorpion.jdiff.exception.MergingValidationError;

public interface MergingHandler<T> {

    @Nullable
    T merge(@Nullable T src, @Nonnull DiffNode diffNode, @Nonnull
            MergingContext mergingContext) throws MergingException;

    default void validateSourceValue(@Nullable Object src, @Nonnull DiffNode diffNode, @Nonnull MergingContext mergingContext)
            throws MergingValidationError {
        if (mergingContext.getObjectDiffMapper().isEnabled(Feature.MergingValidationCheck.VALIDATE_SOURCE_VALUE)) {
            return;
        }
        Diff diff = diffNode.getDiff();
        if (diff == null) {
            return;
        }
        Object expectedSrc = diff.getSrcValue();
        if (src != expectedSrc) {
            if (src == null) {
                throw new MergingValidationError("Expected source value is not null but actual value is");
            }
            if (!src.equals(expectedSrc)) {
                throw new MergingValidationError("Expected source value " + expectedSrc + " does not match the actual value " + src);
            }
        }
    }

}
