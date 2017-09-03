package org.xcorpion.jdiff.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TypeHandler<T> {

    @Nonnull
    String getTypeId();

    default boolean isEqualTo(@Nullable T src, @Nullable T target) {
        if (src == target) {
            return true;
        }
        if (src == null || target == null) {
            return false;
        }
        return src.equals(target);
    }

    default String getObjectId(T obj) {
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    @Nullable
    default T merge(@Nullable T parent, @Nonnull MergeContext mergeContext) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    default DiffNode diff(@Nullable T src, @Nullable T target, @Nonnull DiffContext diffContext) {
        throw new UnsupportedOperationException();
    }

}
