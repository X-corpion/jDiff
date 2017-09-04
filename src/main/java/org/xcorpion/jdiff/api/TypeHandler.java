package org.xcorpion.jdiff.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TypeHandler<T> {

    default boolean isEqualTo(@Nullable T src, @Nullable T target) {
        if (src == target) {
            return true;
        }
        if (src == null || target == null) {
            return false;
        }
        return src.equals(target);
    }

    @Nullable
    default T merge(@Nullable T parent, @Nonnull MergingContext mergingContext) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default DiffNode diff(@Nullable T src, @Nullable T target, @Nonnull DiffContext diffContext) {
        throw new UnsupportedOperationException();
    }

}
