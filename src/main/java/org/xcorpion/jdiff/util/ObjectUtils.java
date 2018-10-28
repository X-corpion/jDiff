package org.xcorpion.jdiff.util;

import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ObjectUtils {

    public static Class<?> inferClass(@Nullable Object srcValue, Object targetValue) {
        return srcValue != null ? srcValue.getClass() : targetValue.getClass();
    }

    @Nullable
    public static Class<?> inferClass(@Nullable Object srcValue, @Nonnull DiffNode diffNode) {
        if (srcValue != null) {
            return srcValue.getClass();
        }
        Diff diff = diffNode.getDiff();
        if (diff == null) {
            return null;
        }
        return inferClass(diff.getSrcValue(), diff.getTargetValue());
    }

}
