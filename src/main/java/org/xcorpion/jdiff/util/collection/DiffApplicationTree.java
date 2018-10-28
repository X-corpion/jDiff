package org.xcorpion.jdiff.util.collection;

import org.xcorpion.jdiff.annotation.TypeHandler;
import org.xcorpion.jdiff.api.*;
import org.xcorpion.jdiff.exception.MergingException;
import org.xcorpion.jdiff.exception.MergingValidationError;
import org.xcorpion.jdiff.internal.model.DefaultMergingContext;
import org.xcorpion.jdiff.util.ObjectUtils;
import org.xcorpion.jdiff.util.reflection.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DiffApplicationTree implements TreeLike<DiffApplicationTree> {

    private static class RemovalPair {
        final int index;
        final Object obj;

        RemovalPair(int index, Object obj) {
            this.index = index;
            this.obj = obj;
        }
    }

    private Type type;
    private Object obj;
    private Object updatedObj;
    private DiffNode diffNode;
    private Set<Object> removedFieldDiffKeys = new HashSet<>();
    private boolean shouldSkip;

    public DiffApplicationTree(Type type, Object obj, DiffNode diffNode) {
        this(type, obj, diffNode, false);
    }

    @SuppressWarnings("WeakerAccess")
    public DiffApplicationTree(Type type, Object obj, DiffNode diffNode, boolean shouldSkip) {
        this.type = type;
        this.obj = obj;
        this.updatedObj = obj;
        this.diffNode = diffNode;
        this.shouldSkip = shouldSkip;
    }

    @Override
    public DiffApplicationTree getNodeValue() {
        return this;
    }

    @SuppressWarnings("unchecked")
    public Object applyDiff(MergingContext mergingContext) {
        if (shouldSkip) {
            return obj;
        }
        ObjectDiffMapper objectDiffMapper = mergingContext.getObjectDiffMapper();
        Set<Feature.MergingStrategy> currentContextStrategies = mergingContext.getMergingStrategies();
        Object src = obj;
        Diff diff = diffNode.getDiff();
        Object target = diff.getTargetValue();
        Diff.Operation op = diff.getOperation();
        if (op == null) {
            throw new IllegalStateException("Unexpected operation: null");
        }
        if (!objectDiffMapper.isEnabled(Feature.MergingHandler.IGNORE_CLASS_TYPE_HANDLER)) {
            MergingHandler<?> mergingHandler = findMergingHandler(this.type, mergingContext);
            if (mergingHandler != null) {
                return mergeUsingCustomHandler(src, diffNode, mergingContext, mergingHandler);
            }
        }
        if (!objectDiffMapper.isEnabled(Feature.MergingHandler.IGNORE_GLOBAL_TYPE_HANDLER)) {
            if (src != null) {
                org.xcorpion.jdiff.api.MergingHandler mergingHandler = objectDiffMapper.getMergingHandler((Class<Object>) src.getClass());
                if (mergingHandler != null) {
                    this.updatedObj = mergeUsingCustomHandler(src, diffNode, mergingContext, mergingHandler);
                    return this.updatedObj;
                }
            }
        }

        // for these ops there's no further actions required
        // they should have been handled at one level up
        if (op == Diff.Operation.REMOVE_VALUE) {
            if (mergingContext.isRootObject()) {
                throw new IllegalStateException("Operation " +
                        Diff.Operation.REMOVE_VALUE + " should not appear at the root level");
            }
            // Upper level call should have handled this so skipping
            return null;
        } else if (op == Diff.Operation.ADD_VALUE) {
            return diff.getTargetValue();
        } else if (op == Diff.Operation.UPDATE_VALUE) {
            if (src != null && target != null && !src.getClass().isAssignableFrom(target.getClass())) {
                String message = String.format("Source class (%s) is not compatible with target (%s). " +
                        "The source class is expected to be either equal to or superclass of target.",
                        src.getClass().getName(),
                        target.getClass().getName());
                throw new MergingException(message);
            }
            if (objectDiffMapper.isEnabled(Feature.MergingValidationCheck.VALIDATE_SOURCE_VALUE)) {
                Object expectedSrc = diff.getSrcValue();
                validateSourceValue(objectDiffMapper, src, expectedSrc);
            }
            return diff.getTargetValue();
        }

        MergingContext nextLevelMergingContext = mergingContext;
        if (mergingContext.isRootObject()) {
            nextLevelMergingContext = new DefaultMergingContext(
                    objectDiffMapper,
                    currentContextStrategies,
                    false
            );
        }

        if (src == null) {
            switch (op) {
                case NO_OP:
                    return null;
                default:
                    throw new IllegalStateException("Operation " +
                            op + " should not appear at the root level when input is null");
            }
        }

        if (src.getClass().isArray()) {
            // for array we only expect resizing or no op
            switch (op) {
                case NO_OP: {
                    Object newArray = src;
                    if (objectDiffMapper.isMergingStrategyEnabled(
                            Feature.MergingStrategy.SHALLOW_CLONE_SOURCE_COLLECTIONS, currentContextStrategies)) {
                        newArray = cloneArray(src);
                    }
                    this.updatedObj = newArray;
                    handleArrayChildUpdates(newArray, mergingContext);
                    return newArray;
                }
                case RESIZE_ARRAY: {
                    Integer newLength = (Integer) diff.getTargetValue();
                    if (newLength == null || newLength < 0) {
                        throw new MergingException("Illegal new array size: " + newLength);
                    }
                    int currentLength = Array.getLength(src);
                    Object newArray = Array.newInstance(src.getClass().getComponentType(), newLength);
                    //noinspection SuspiciousSystemArraycopy
                    System.arraycopy(src, 0, newArray, 0, Math.min(currentLength, newLength));
                    this.updatedObj = newArray;
                    handleArrayChildUpdates(newArray, nextLevelMergingContext);
                    return newArray;
                }
                default:
                    throw new MergingException("Illegal operation for array: " + op);
            }
        }
        if (mergingContext.isRootObject()) {
            this.updatedObj = cloneSrcIfNeeded(src, mergingContext);
        }
        if (this.updatedObj instanceof List) {
            if (op != Diff.Operation.NO_OP) {
                throw new MergingException("Illegal operation for list: " + op);
            }
            handleListChildUpdates(this.updatedObj, nextLevelMergingContext);
        } else if (this.updatedObj instanceof Set) {
            if (op != Diff.Operation.NO_OP) {
                throw new MergingException("Illegal operation for set: " + op);
            }
            handleSetChildUpdates(this.updatedObj, nextLevelMergingContext);
        } else if (this.updatedObj instanceof Map) {
            if (op != Diff.Operation.NO_OP) {
                throw new MergingException("Illegal operation for map: " + op);
            }
            handleMapChildUpdates(this.updatedObj, nextLevelMergingContext);
        } else if (this.updatedObj instanceof Iterable) {
            throw new UnsupportedOperationException("Sorry, auto iterable merging is not supported. " +
                    "Please implement your type handler to handle merging.");
        } else {
            handleObjectChildUpdates(this.updatedObj, nextLevelMergingContext);
        }
        return this.updatedObj;
    }

    private static Object cloneSrcIfNeeded(Object src, MergingContext mergingContext) {
        ObjectDiffMapper objectDiffMapper = mergingContext.getObjectDiffMapper();
        Set<Feature.MergingStrategy> currentContextStrategies = mergingContext.getMergingStrategies();
        if (objectDiffMapper.isMergingStrategyEnabled(
                Feature.MergingStrategy.DEEP_CLONE_SOURCE, currentContextStrategies)) {
            return ReflectionUtils.deepClone(src);
        }
        else if (objectDiffMapper.isMergingStrategyEnabled(
                Feature.MergingStrategy.SHALLOW_CLONE_SOURCE_ROOT, currentContextStrategies)) {
            return ReflectionUtils.shallowClone(src);
        }
        else if (objectDiffMapper.isMergingStrategyEnabled(
                Feature.MergingStrategy.SHALLOW_CLONE_SOURCE_COLLECTIONS, currentContextStrategies)) {
            if (src instanceof Collection) {
                return ReflectionUtils.shallowClone(src);
            }
            if (src != null && src.getClass().isArray()) {
                return cloneArray(src);
            }
        }
        return src;
    }

    @Nullable
    private MergingHandler<?> findMergingHandler(@Nonnull Field field, @Nonnull MergingContext mergingContext) {
        TypeHandler typeHandler = field.getAnnotation(TypeHandler.class);
        if (typeHandler != null && typeHandler.mergeUsing() != TypeHandler.None.class) {
            Class<? extends org.xcorpion.jdiff.api.MergingHandler> mergingHandlerClass = typeHandler.mergeUsing();
            try {
                return mergingHandlerClass.newInstance();
            }
            catch (Throwable e) {
                throw new MergingException("Failed to instantiate merging handler " + mergingHandlerClass +
                        " for field " + field.getName(), e);
            }
        }
        return findMergingHandler(field.getGenericType(), mergingContext);
    }

    @Nullable
    private MergingHandler<?> findMergingHandler(@Nonnull Type type, @Nonnull MergingContext mergingContext) {
        ObjectDiffMapper mapper = mergingContext.getObjectDiffMapper();
        return mapper.getMergingHandler(type);
    }

    @SuppressWarnings("unchecked")
    private static Object mergeUsingCustomHandler(@Nonnull Object src, @Nonnull DiffNode diffNode,
            @Nonnull MergingContext mergingContext, @Nonnull org.xcorpion.jdiff.api.MergingHandler mergingHandler) {
        src = cloneSrcIfNeeded(src, mergingContext);
        return mergingHandler.merge(src, diffNode, mergingContext);
    }

    private static Object cloneArray(Object src) {
        int len = Array.getLength(src);
        Object newArray = Array.newInstance(src.getClass().getComponentType(), len);
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(src, 0, newArray, 0, len);
        return newArray;
    }

    private void handleArrayChildUpdates(Object parent, MergingContext mergingContext) {
        for (Map.Entry<Object, DiffNode> entry : diffNode.getFieldDiffs().entrySet()) {
            int index = (int) entry.getKey();
            Diff diff = entry.getValue().getDiff();
            switch (diff.getOperation()) {
                case NO_OP:
                    break;
                // update is basically add plus potential check
                case UPDATE_VALUE:
                    ObjectDiffMapper diffMapper = mergingContext.getObjectDiffMapper();
                    if (diffMapper.isEnabled(Feature.MergingValidationCheck.VALIDATE_SOURCE_VALUE)) {
                        Object expectedSrc = diff.getSrcValue();
                        Object src = Array.get(parent, index);
                        validateSourceValue(diffMapper, src, expectedSrc);
                    }
                case ADD_VALUE:
                    Array.set(parent, index, diff.getTargetValue());
                    break;
                case REMOVE_VALUE:
                    removedFieldDiffKeys.add(entry.getKey());
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleListChildUpdates(Object parent, MergingContext mergingContext) {
        List<Object> list = (List<Object>) parent;
        List<RemovalPair> itemsToRemove = new ArrayList<>();
        for (Map.Entry<Object, DiffNode> entry : diffNode.getFieldDiffs().entrySet()) {
            int index = (int) entry.getKey();
            Diff diff = entry.getValue().getDiff();
            switch (diff.getOperation()) {
                case NO_OP:
                    break;
                case UPDATE_VALUE:
                    ObjectDiffMapper diffMapper = mergingContext.getObjectDiffMapper();
                    if (diffMapper.isEnabled(Feature.MergingValidationCheck.VALIDATE_SOURCE_VALUE)) {
                        Object expectedSrc = diff.getSrcValue();
                        Object src = list.get(index);
                        validateSourceValue(diffMapper, src, expectedSrc);
                    }
                    list.set(index, diff.getTargetValue());
                    break;
                case ADD_VALUE:
                    list.add(diff.getTargetValue());
                    break;
                case REMOVE_VALUE:
                    itemsToRemove.add(new RemovalPair(index, diff.getSrcValue()));
                    removedFieldDiffKeys.add(entry.getKey());
                    break;
            }
        }

        itemsToRemove.sort((p1, p2) -> p2.index - p1.index);
        for (RemovalPair pair : itemsToRemove) {
            list.remove(pair.index);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSetChildUpdates(Object parent, MergingContext mergingContext) {
        Set<Object> set = (Set<Object>) parent;
        for (Map.Entry<Object, DiffNode> entry : diffNode.getFieldDiffs().entrySet()) {
            Diff diff = entry.getValue().getDiff();
            switch (diff.getOperation()) {
                case NO_OP:
                    break;
                case UPDATE_VALUE:
                    ObjectDiffMapper diffMapper = mergingContext.getObjectDiffMapper();
                    if (diffMapper.isEnabled(Feature.MergingValidationCheck.VALIDATE_SOURCE_VALUE)) {
                        Object expectedSrc = diff.getSrcValue();
                        if (!set.contains(expectedSrc)) {
                            throw new MergingValidationError("Set does not contain expected source value: " + expectedSrc);
                        }
                    }
                case ADD_VALUE:
                    set.add(diff.getTargetValue());
                    break;
                case REMOVE_VALUE:
                    set.remove(diff.getSrcValue());
                    removedFieldDiffKeys.add(entry.getKey());
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMapChildUpdates(Object parent, MergingContext mergingContext) {
        Map<Object, Object> map = (Map<Object, Object>) parent;
        for (Map.Entry<Object, DiffNode> entry : diffNode.getFieldDiffs().entrySet()) {
            Object key = entry.getKey();
            DiffNode diffWrapper = entry.getValue();
            Diff diff = diffWrapper.getDiff();
            ObjectDiffMapper diffMapper = mergingContext.getObjectDiffMapper();
            switch (diff.getOperation()) {
                case NO_OP:
                    break;
                case UPDATE_VALUE:
                    if (diffMapper.isEnabled(Feature.MergingValidationCheck.VALIDATE_SOURCE_VALUE)) {
                        Object expectedSrc = diff.getSrcValue();
                        Object src = map.get(key);
                        if (src != expectedSrc && src == null) {
                            throw new MergingValidationError("Map does not contain expected source value: " + expectedSrc);
                        }
                    }
                case ADD_VALUE:
                    map.put(key, diff.getTargetValue());
                    break;
                case REMOVE_VALUE:
                    if (diffMapper.isEnabled(Feature.MergingValidationCheck.VALIDATE_SOURCE_VALUE)) {
                        if (!map.containsKey(key)) {
                            throw new MergingValidationError("Unable to remove value: Map does not contain expected key: " + key);
                        }
                    }
                    map.remove(key);
                    removedFieldDiffKeys.add(entry.getKey());
                    break;
            }
        }
    }

    private void handleObjectChildUpdates(Object parent, MergingContext mergingContext) {
        for (Map.Entry<Object, DiffNode> entry : diffNode.getFieldDiffs().entrySet()) {
            String fieldName = (String) entry.getKey();
            DiffNode currentLevelDiffNode = entry.getValue();
            Diff diff = currentLevelDiffNode.getDiff();
            Field field = ReflectionUtils.getField(parent, fieldName);
            ObjectDiffMapper objectDiffMapper = mergingContext.getObjectDiffMapper();
            if (field == null) {
                if (objectDiffMapper.isEnabled(Feature.MergingValidationCheck.VALIDATE_OBJECT_FIELD_EXISTENCE)) {
                    throw new MergingException("Unable to find " + fieldName + " in " + parent.getClass().getName());
                }
                continue;
            }
            field.setAccessible(true);
            if (!mergingContext.getObjectDiffMapper().isEnabled(Feature.MergingHandler.IGNORE_FIELD_TYPE_HANDLER)) {
                try {
                    Object fieldSrc = field.get(parent);
                    @SuppressWarnings("unchecked")
                    MergingHandler<?> mergingHandler = findMergingHandler(field, mergingContext);
                    if (mergingHandler != null) {
                        Object result = mergeUsingCustomHandler(fieldSrc,
                                currentLevelDiffNode, mergingContext, mergingHandler);
                        field.set(parent, result);
                        removedFieldDiffKeys.add(fieldName);
                        continue;
                    }
                } catch (IllegalAccessException e) {
                    throw new MergingException("Failed to access field " + fieldName + " in " + parent.getClass().getName(), e);
                }
            }
            switch (diff.getOperation()) {
                case NO_OP:
                    break;
                case UPDATE_VALUE:
                    field.setAccessible(true);
                    Object fieldTarget = diff.getTargetValue();
                    try {
                        field.set(parent, fieldTarget);
                    } catch (IllegalAccessException e) {
                        throw new MergingException("Failed to set " + fieldName + " in " + parent.getClass().getName(), e);
                    }
                    break;
                default:
                    String message = String.format("Unexpected operation: %s to be applied to field %s in %s",
                            diff.getOperation(),
                            fieldName,
                            parent.getClass().getName());
                    throw new IllegalStateException(message);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void validateSourceValue(ObjectDiffMapper objectDiffMapper, Object src, Object expectedSrc) {
        if (src != expectedSrc) {
            if (src == null) {
                throw new MergingValidationError("Expected source value is not null but actual value is");
            } else if (expectedSrc == null) {
                throw new MergingValidationError("Expected source value is null but actual value is not");
            }

            EqualityChecker<Object> equalityChecker = objectDiffMapper.getEqualityChecker((Class<Object>) src.getClass());
            if (equalityChecker != null) {
                if (!equalityChecker.isEqualTo(src, expectedSrc)) {
                    throw new MergingValidationError("Expected value not equal to actual");
                }
            }
            if (objectDiffMapper.isEnabled(Feature.EqualityCheck.USE_EQUALS)) {
                if (src.equals(expectedSrc)) {
                    throw new MergingValidationError("Expected value not equal to actual");
                }
            }
            else if (objectDiffMapper.isEnabled(Feature.EqualityCheck.USE_HASHCODE)) {
                if (src.hashCode() != expectedSrc.hashCode()) {
                    throw new MergingValidationError("Expected value not equal to actual: hash code mismatch");
                }
            }
        }
    }

    @Override
    public Iterable<DiffApplicationTree> getChildren() {
        return () -> new Iterator<DiffApplicationTree>() {

            private Iterator<Map.Entry<Object, DiffNode>> diffIter;
            int index = 0;
            Iterator<Object> collectionIter = null;

            {
                Map<Object, DiffNode> childDiffs = diffNode.getFieldDiffs();
                if (childDiffs == null) {
                    diffIter = Collections.emptyListIterator();
                } else {
                    List<Map.Entry<Object, DiffNode>> entries = new ArrayList<>(childDiffs.entrySet());
                    // for array and iterables we need to sort the indices otherwise we might end up jumping around
                    if ((updatedObj != null && updatedObj.getClass().isArray()) ||
                            updatedObj instanceof Iterable) {
                        entries.sort((e1, e2) -> {
                            int index1 = (int) e1.getKey();
                            int index2 = (int) e2.getKey();
                            return index1 - index2;
                        });
                    }
                    diffIter = entries.iterator();
                }
            }

            @Override
            public boolean hasNext() {
                return diffIter.hasNext();
            }

            @SuppressWarnings("unchecked, ConstantConditions")
            @Override
            public DiffApplicationTree next() {
                if (updatedObj == null) {
                    throw new MergingException("Invalid state: attempting to apply child diff to null object");
                }
                Map.Entry<Object, DiffNode> diffEntry = diffIter.next();
                Object fieldKey = diffEntry.getKey();
                DiffNode diffNode = diffEntry.getValue();
                boolean shouldSkip = removedFieldDiffKeys.contains(fieldKey);
                Class<?> currentObjClass = updatedObj.getClass();
                if (currentObjClass.isArray()) {
                    Class<?> arrayElementsType = currentObjClass.getComponentType();
                    if (!(fieldKey instanceof Integer)) {
                        throw new MergingException("Expect index value for array object. Got: " + fieldKey);
                    }
                    if (shouldSkip) {
                        return new DiffApplicationTree(null, null, diffNode, true);
                    }
                    Integer key = (Integer) fieldKey;
                    Object fieldObj = Array.get(updatedObj, key);
                    return new DiffApplicationTree(arrayElementsType, fieldObj, diffNode, shouldSkip);
                } else if (updatedObj instanceof Set) {
                    if (collectionIter == null) {
                        collectionIter = ((Set<Object>) updatedObj).iterator();
                    }
                    Object fieldObj = collectionIter.next();
                    return new DiffApplicationTree(ObjectUtils.inferClass(fieldObj, diffNode),
                            fieldObj, diffNode, shouldSkip);
                } else if (updatedObj instanceof Map) {
                    Map<Object, Object> map = (Map<Object, Object>) updatedObj;
                    Object fieldObj = map.get(fieldKey);
                    return new DiffApplicationTree(ObjectUtils.inferClass(fieldObj, diffNode),
                            fieldObj, diffNode, shouldSkip);
                } else if (updatedObj instanceof Iterable) {
                    if (shouldSkip) {
                        return new DiffApplicationTree(null, null, diffNode, true);
                    }
                    if (collectionIter == null) {
                        collectionIter = ((Iterable<Object>) updatedObj).iterator();
                    }
                    Object fieldObj = collectionIter.next();
                    if (!(fieldKey instanceof Integer)) {
                        throw new MergingException("Expect index value for iterable object. Got: " + fieldKey);
                    }
                    Integer key = (Integer) fieldKey;
                    while (index++ != key) {
                        fieldObj = collectionIter.next();
                    }
                    return new DiffApplicationTree(ObjectUtils.inferClass(fieldObj, diffNode),
                            fieldObj, diffNode, shouldSkip);
                }
                if (!(fieldKey instanceof String)) {
                    throw new MergingException("Expect field name. Got: " + fieldKey);
                }
                String fieldName = (String) fieldKey;
                Field field = ReflectionUtils.getField(updatedObj, fieldName);
                if (field == null) {
                    throw new MergingException("Unable to find " + fieldName +
                            " in class " + updatedObj.getClass().getName());
                }
                field.setAccessible(true);
                Object fieldObj;
                try {
                    fieldObj = field.get(updatedObj);
                } catch (IllegalAccessException e) {
                    throw new MergingException("Unable to access " + fieldName +
                            " in class " + updatedObj.getClass().getName(), e);
                }
                Diff diff = diffNode.getDiff();
                Object targetValue = diff == null ? null : diff.getTargetValue();
                return new DiffApplicationTree(
                        ReflectionUtils.guessType(field, fieldObj, targetValue),
                        fieldObj, diffNode, shouldSkip);
            }
        };
    }
}
