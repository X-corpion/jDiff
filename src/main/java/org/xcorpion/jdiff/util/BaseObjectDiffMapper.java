package org.xcorpion.jdiff.util;

import org.xcorpion.jdiff.api.*;
import org.xcorpion.jdiff.exception.DiffException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;

@SuppressWarnings("unchecked, WeakerAccess")
public abstract class BaseObjectDiffMapper implements ObjectDiffMapper {

    protected static final Object DELETION_MARK = new Object() {
        @Override
        public String toString() {
            return "<DELETE>";
        }
    };

    private Map<Class<? extends Feature>, List<Feature>> features = new HashMap<>();
    private Map<Class<?>, EqualityChecker<?>> equalityCheckers = new HashMap<>();
    private Map<Class<?>, DiffingHandler<?>> diffingHandlers = new HashMap<>();
    private Map<Class<?>, MergingHandler<?>> mergingHandlers = new HashMap<>();
    private static final Set<Class<?>> PRIMITIVE_TYPES = new HashSet<>();

    static {
        PRIMITIVE_TYPES.add(Boolean.class);
        PRIMITIVE_TYPES.add(boolean.class);
        PRIMITIVE_TYPES.add(Byte.class);
        PRIMITIVE_TYPES.add(byte.class);
        PRIMITIVE_TYPES.add(Short.class);
        PRIMITIVE_TYPES.add(short.class);
        PRIMITIVE_TYPES.add(Integer.class);
        PRIMITIVE_TYPES.add(int.class);
        PRIMITIVE_TYPES.add(Long.class);
        PRIMITIVE_TYPES.add(long.class);
        PRIMITIVE_TYPES.add(Float.class);
        PRIMITIVE_TYPES.add(float.class);
        PRIMITIVE_TYPES.add(Double.class);
        PRIMITIVE_TYPES.add(double.class);
        PRIMITIVE_TYPES.add(String.class);
    }

    protected BaseObjectDiffMapper() {
        configureDefaultFeatures();
    }

    private void configureDefaultFeatures() {
        this.enable(Feature.EqualityCheck.USE_EQUALS);
    }

    @Override
    public <T> T applyDiff(@Nullable T src, @Nonnull DiffNode diffs) {
        return applyDiff(src, diffs, Collections.emptySet());
    }

    @Override
    public <T> EqualityChecker<T> getEqualityChecker(@Nonnull Class<T> cls) {
        return (EqualityChecker<T>) equalityCheckers.get(cls);
    }

    @Override
    public <T> DiffingHandler<T> getDiffingHandler(@Nonnull Class<T> cls) {
        return (DiffingHandler<T>) diffingHandlers.get(cls);
    }

    @Override
    public <T> MergingHandler<T> getMergingHandler(@Nonnull Class<T> cls) {
        return (MergingHandler<T>) mergingHandlers.get(cls);
    }

    @Override
    public <T> ObjectDiffMapper registerEqualityChecker(@Nonnull Class<T> cls, @Nonnull EqualityChecker<? super T> equalityChecker) {
        equalityCheckers.put(cls, equalityChecker);
        return this;
    }

    @Override
    public <T> ObjectDiffMapper registerDiffingHandler(@Nonnull Class<T> cls, @Nonnull DiffingHandler<? super T> diffingHandler) {
        diffingHandlers.put(cls, diffingHandler);
        return this;
    }

    @Override
    public <T> ObjectDiffMapper registerMergingHandler(@Nonnull Class<T> cls, @Nonnull MergingHandler<? super T> mergingHandler) {
        mergingHandlers.put(cls, mergingHandler);
        return this;
    }

    @Override
    public ObjectDiffMapper enable(@Nonnull Feature feature) {
        if (feature.allowMultiple()) {
            List<Feature> featureList = features.computeIfAbsent(feature.getClass(), k -> new ArrayList<>());
            featureList.add(feature);
        } else {
            features.put(feature.getClass(), Collections.singletonList(feature));
        }
        return this;
    }

    @Override
    public ObjectDiffMapper disable(@Nonnull Feature feature) {
        List<Feature> featureList = features.get(feature.getClass());
        featureList.remove(feature);
        return this;
    }

    @Override
    public boolean isEnabled(@Nonnull Feature feature) {
        List<Feature> values = features.get(feature.getClass());
        return values != null && values.contains(feature);
    }

    Class<?> determineClass(@Nullable Object src, @Nullable Object target) {
        if (src == null && target == null) {
            throw new IllegalStateException("src and target cannot both be null");
        }
        return src != null ? src.getClass() : target.getClass();
    }

    boolean isEqualTo(@Nullable Object src, @Nullable Object target) {
        if (src == target) {
            return true;
        }
        if (src == null || target == null) {
            return false;
        }
        Class<?> type = determineClass(src, target);
        EqualityChecker<Object> typeHandler = (EqualityChecker<Object>) getEqualityChecker(type);
        if (typeHandler != null) {
            return typeHandler.isEqualTo(src, target);
        }
        if (isEnabled(Feature.EqualityCheck.USE_HASHCODE)) {
            return src.hashCode() == target.hashCode();
        }
        return src.equals(target);
    }

    protected boolean isPrimitive(Object obj) {
        if (obj == null) {
            return true;
        }
        Class<?> clz = obj.getClass();
        return PRIMITIVE_TYPES.contains(clz);
    }

    protected DiffNode createArrayDiffGroup(@Nonnull Object src, @Nonnull Object target) throws DiffException {
        if (!src.getClass().equals(target.getClass())) {
            throw new DiffException("Cannot diff two arrays with different types: not supported");
        }
        int srcSize = Array.getLength(src);
        int targetSize = Array.getLength(target);
        if (srcSize != targetSize) {
            return new DiffNode(new Diff(Diff.Operation.RESIZE_ARRAY, srcSize, targetSize));
        }
        return new DiffNode(new Diff(Diff.Operation.NO_OP, srcSize, targetSize));
    }

    protected DiffNode createPrimitiveUpdateDiffGroup(@Nullable Object src, @Nullable Object target) {
        Diff diff = new Diff(Diff.Operation.UPDATE_VALUE, src, target);
        return new DiffNode(diff);
    }

    @Override
    public boolean isMergingStrategyEnabled(
            @Nonnull Feature.MergingStrategy strategy,
            @Nullable Set<Feature.MergingStrategy> oneOffStrategies
    ) {
        if (oneOffStrategies != null) {
            if (oneOffStrategies.contains(strategy)) {
                return true;
            }
        }
        return isEnabled(strategy);
    }
}
