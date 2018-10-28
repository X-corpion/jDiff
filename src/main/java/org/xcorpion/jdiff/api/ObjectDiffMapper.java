package org.xcorpion.jdiff.api;

import java.lang.reflect.Type;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ObjectDiffMapper {

    @Nonnull
    <T> DiffNode diff(@Nullable T src, @Nullable T target);

    <T> T applyDiff(@Nullable T src, @Nonnull DiffNode diffs);

    <T> T applyDiff(@Nullable T src, @Nonnull DiffNode diffs, @Nonnull Set<Feature.MergingStrategy> mergingStrategies);

    <T> EqualityChecker<T> getEqualityChecker(@Nonnull Class<T> cls);

    <T> DiffingHandler<T> getDiffingHandler(@Nonnull Class<T> cls);

    <T> DiffingHandler<T> getDiffingHandler(@Nonnull Type type);

    <T> MergingHandler<T> getMergingHandler(@Nonnull Class<T> cls);

    <T> MergingHandler<T> getMergingHandler(@Nonnull Type type);

    <T> ObjectDiffMapper registerEqualityChecker(@Nonnull Class<T> cls, @Nonnull EqualityChecker<? super T> equalityChecker);

    <T> ObjectDiffMapper registerDiffingHandler(@Nonnull Class<T> cls, @Nonnull DiffingHandler<? super T> diffingHandler);

    ObjectDiffMapper registerDiffingHandler(@Nonnull AbstractDiffingHandler<?> diffingHandler);

    <T> ObjectDiffMapper registerMergingHandler(@Nonnull Class<T> cls, @Nonnull MergingHandler<? super T> mergingHandler);

    ObjectDiffMapper registerMergingHandler(@Nonnull AbstractMergingHandler<?> mergingHandler);

    ObjectDiffMapper enable(@Nonnull Feature feature);

    ObjectDiffMapper disable(@Nonnull Feature feature);

    boolean isEnabled(@Nonnull Feature feature);

    boolean isMergingStrategyEnabled(
            @Nonnull Feature.MergingStrategy strategy,
            @Nullable Set<Feature.MergingStrategy> oneOffStrategies
    );

}
