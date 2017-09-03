package org.xcorpion.jdiff.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ObjectDiffMapper {

    @Nonnull
    <T> DiffNode diff(@Nullable T src, @Nullable T target);

    <T> T applyDiff(@Nullable T src, @Nonnull DiffNode diffs);

    <T> T applyDiffInPlace(@Nullable T src, @Nonnull DiffNode diffs);

    <T> TypeHandler<T> getTypeHandler(@Nonnull Class<T> cls);

    <T> ObjectDiffMapper registerTypeHandler(@Nonnull Class<T> cls, @Nonnull TypeHandler<? super T> typeHandler);

    ObjectDiffMapper enable(@Nonnull Feature feature);

    ObjectDiffMapper disable(@Nonnull Feature feature);

    boolean isEnabled(@Nonnull Feature feature);

}
