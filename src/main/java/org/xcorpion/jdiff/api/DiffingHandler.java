package org.xcorpion.jdiff.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DiffingHandler<T> {

    @Nonnull
    DiffNode diff(@Nullable T src, @Nullable T target, @Nonnull DiffContext diffContext);
}
