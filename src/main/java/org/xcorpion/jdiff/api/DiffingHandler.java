package org.xcorpion.jdiff.api;

import javax.annotation.Nonnull;

public interface DiffingHandler<T> {

    @Nonnull
    DiffNode diff(@Nonnull T src, @Nonnull T target, @Nonnull DiffingContext diffingContext);
}
