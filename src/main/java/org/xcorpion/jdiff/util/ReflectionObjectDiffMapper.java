package org.xcorpion.jdiff.util;

import org.xcorpion.jdiff.annotation.TypeHandler;
import org.xcorpion.jdiff.api.*;
import org.xcorpion.jdiff.exception.DiffException;
import org.xcorpion.jdiff.exception.MergingException;
import org.xcorpion.jdiff.internal.model.DefaultDiffingContext;
import org.xcorpion.jdiff.internal.model.DefaultMergingContext;
import org.xcorpion.jdiff.util.collection.DiffApplicationTree;
import org.xcorpion.jdiff.util.collection.Iterables;
import org.xcorpion.jdiff.util.collection.Tree;
import org.xcorpion.jdiff.util.reflection.ReflectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ReflectionObjectDiffMapper
        extends BaseObjectDiffMapper
        implements ObjectDiffMapper {

    @Override
    @Nonnull
    public <T> DiffNode diff(@Nullable T src, @Nullable T target) {
        if (isEqualTo(src, target)) {
            return new DiffNode();
        }
        Tree<DiffNode> diffTree = createNextDiffTreeNode(src, target);
        Iterable<DiffNode> diffGroups = diffTree.preOrderTraversal();
        Iterator<DiffNode> iter = diffGroups.iterator();
        DiffNode root = iter.next();
        // this introduces side effects as it builds the tree as traversal goes
        while (iter.hasNext()) {
            iter.next();
        }
        return root;
    }

    @Nonnull
    private DiffNode createDiffGroupOneLevel(@Nullable Object src, @Nullable Object target) {
        if (target == DELETION_MARK) {
            return new DiffNode(new Diff(Diff.Operation.REMOVE_VALUE, src, null));
        }
        if (src == null || target == null) {
            return new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE, src, target));
        }
        if (isPrimitive(src)) {
            return createPrimitiveUpdateDiffGroup(src, target);
        }
        if (src.getClass().isArray()) {
            return createArrayDiffGroup(src, target);
        }
        return new DiffNode(new Diff(Diff.Operation.NO_OP, null, null));
    }

    @Nonnull
    private <T> Iterable<Tree<DiffNode>> generateChildDiffGroups(@Nonnull final DiffNode parentDiffNode,
            @Nullable final T src, @Nullable final T target) {
        if (target == null || isPrimitive(target)) {
            return Iterables.empty();
        }
        return createFieldDiffIterable(parentDiffNode, src, target);
    }

    private Iterable<Tree<DiffNode>> createFieldDiffIterable(@Nonnull DiffNode parentDiffNode,
            @Nullable final Object src, final @Nonnull Object target) {
        Class<?> diffClass = determineClass(src, target);
        if (src == null) {
            return Collections.singletonList(new Tree<>(
                    new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE, null, target))
            ));
        }

        if (diffClass.isArray()) {
            return createArrayDiffIterable(parentDiffNode, src, target);
        }
        if (src instanceof Set) {
            return createSetDiffIterable(parentDiffNode, src, target);
        }
        if (src instanceof Map) {
            return createMapDiffIterable(parentDiffNode, src, target);
        }
        if (src instanceof Iterable) {
            return createOrderedDiffIterable(parentDiffNode, src, target);
        }

        List<Field> fields = ReflectionUtils.getAllFieldsRecursive(diffClass);

        return () -> new Iterator<Tree<DiffNode>>() {
            int index = 0;
            Field field = null;
            Object srcFieldValue = null;
            Object targetFieldValue = null;
            DiffingHandler<Object> fieldDiffingHandler = null;

            @Override
            public boolean hasNext() {
                fieldDiffingHandler = null;
                for (; index < fields.size(); index++) {
                    srcFieldValue = null;
                    targetFieldValue = null;
                    field = fields.get(index);
                    if (isEnabled(Feature.IgnoreFields.TRANSIENT)) {
                        if (Modifier.isTransient(field.getModifiers())) {
                            continue;
                        }
                    }
                    if (isEnabled(Feature.IgnoreFields.INACCESSIBLE) && !field.isAccessible()) {
                        continue;
                    }
                    field.setAccessible(true);
                    try {
                        srcFieldValue = field.get(src);
                    } catch (IllegalAccessException e) {
                        throw new DiffException("Failed to access field " + field.getName() +
                                " in " + src.getClass().getName(), e);
                    }
                    try {
                        targetFieldValue = field.get(target);
                    } catch (IllegalAccessException e) {
                        throw new DiffException("Failed to access field " + field.getName() +
                                " in " + target.getClass().getName(), e);
                    }
                    if (isEqualTo(srcFieldValue, targetFieldValue)) {
                        continue;
                    }
                    index++;
                    if (isEnabled(Feature.DiffingHandler.IGNORE_FIELD_TYPE_HANDLER)) {
                        TypeHandler typeHandler = field.getAnnotation(TypeHandler.class);
                        if (typeHandler != null && typeHandler.diffUsing() != TypeHandler.None.class) {
                            try {
                                //noinspection unchecked
                                fieldDiffingHandler = (DiffingHandler<Object>) typeHandler.diffUsing().newInstance();
                            }
                            catch (Throwable e) {
                                String message = String.format("Failed to instantiate diffing handler %s for %s in %s",
                                        typeHandler.diffUsing().getName(),
                                        field.getName(),
                                        diffClass.getName());
                                throw new DiffException(message);
                            }
                        }
                    }
                    if (isEnabled(Feature.DiffingHandler.IGNORE_GLOBAL_TYPE_HANDLER)) {
                        //noinspection unchecked
                        fieldDiffingHandler = getDiffingHandler((Class<Object>) determineClass(srcFieldValue, targetFieldValue));
                    }
                    return true;
                }
                return false;
            }

            @Override
            public Tree<DiffNode> next() {
                if (fieldDiffingHandler != null) {
                    DiffNode diffNode = fieldDiffingHandler.diff(srcFieldValue, targetFieldValue,
                            new DefaultDiffingContext(ReflectionObjectDiffMapper.this));
                    return new Tree<>(diffNode);
                }
                Tree<DiffNode> nextDiffTreeNode = createNextDiffTreeNode(srcFieldValue, targetFieldValue);
                parentDiffNode.addFieldDiff(field.getName(), nextDiffTreeNode.getNodeValue());
                return nextDiffTreeNode;
            }
        };
    }

    private Iterable<Tree<DiffNode>> createArrayDiffIterable(@Nonnull DiffNode parentDiffNode,
            @Nonnull final Object src, @Nonnull final Object target) {
        int srcArraySize = Array.getLength(src);
        int targetArraySize = Array.getLength(target);
        int maxSize = Math.max(srcArraySize, targetArraySize);

        return () -> new Iterator<Tree<DiffNode>>() {
            int index = 0;
            Object srcValue = null;
            Object targetValue = null;

            @Override
            public boolean hasNext() {
                for (; index < maxSize; index++) {
                    srcValue = null;
                    targetValue = null;
                    if (index < srcArraySize) {
                        srcValue = Array.get(src, index);
                    }
                    if (index < targetArraySize) {
                        targetValue = Array.get(target, index);
                    }
                    if (isEqualTo(srcValue, targetValue)) {
                        continue;
                    }
                    index++;
                    return true;
                }
                return false;
            }

            @Override
            public Tree<DiffNode> next() {
                int diffIndex = index - 1;
                if (srcValue != null && targetValue != null) {
                    Tree<DiffNode> nextDiffTreeNode = createNextDiffTreeNode(srcValue, targetValue);
                    parentDiffNode.addFieldDiff(diffIndex, nextDiffTreeNode.getNodeValue());
                    return nextDiffTreeNode;
                }
                DiffNode nextLevelRoot;
                if (diffIndex >= targetArraySize) {
                    nextLevelRoot = createDiffGroupOneLevel(srcValue, DELETION_MARK);
                } else {
                    if (targetValue == null) {
                        nextLevelRoot = createDiffGroupOneLevel(srcValue, null);
                    } else {
                        nextLevelRoot = createDiffGroupOneLevel(null, targetValue);
                    }
                }
                parentDiffNode.addFieldDiff(diffIndex, nextLevelRoot);
                return new Tree<>(nextLevelRoot);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Iterable<Tree<DiffNode>> createOrderedDiffIterable(@Nonnull DiffNode parentDiffNode,
            @Nonnull Object src, @Nonnull Object target) {
        Iterable<Object> srcIterable = (Iterable<Object>) src;
        Iterable<Object> targetIterable = (Iterable<Object>) target;

        return () -> new Iterator<Tree<DiffNode>>() {
            Object srcValue = null;
            Object targetValue = null;
            Iterator<Object> srcIter = srcIterable.iterator();
            Iterator<Object> targetIter = targetIterable.iterator();
            int index = 0;
            boolean srcIterHasNext = false;
            boolean targetIterHasNext = false;

            @Override
            public boolean hasNext() {
                while (true) {
                    srcValue = null;
                    targetValue = null;
                    srcIterHasNext = srcIter.hasNext();
                    targetIterHasNext = targetIter.hasNext();
                    if (!srcIterHasNext && !targetIterHasNext) {
                        return false;
                    }
                    if (srcIterHasNext) {
                        srcValue = srcIter.next();
                    }
                    if (targetIterHasNext) {
                        targetValue = targetIter.next();
                    }
                    if (isEqualTo(srcValue, targetValue)) {
                        index++;
                        continue;
                    }
                    index++;
                    return true;
                }
            }

            @Override
            public Tree<DiffNode> next() {
                int diffIndex = index - 1;
                DiffNode nextLevelRoot;

                if (srcIterHasNext && targetIterHasNext) {
                    Tree<DiffNode> nextDiffTreeNode = createNextDiffTreeNode(srcValue, targetValue);
                    parentDiffNode.addFieldDiff(diffIndex, nextDiffTreeNode.getNodeValue());
                    return nextDiffTreeNode;
                } else if (!targetIterHasNext) {
                    nextLevelRoot = new DiffNode(new Diff(Diff.Operation.REMOVE_VALUE, srcValue, null));
                } else {
                    nextLevelRoot = new DiffNode(new Diff(Diff.Operation.ADD_VALUE, null, targetValue));
                }
                parentDiffNode.addFieldDiff(diffIndex, nextLevelRoot);
                return new Tree<>(nextLevelRoot);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Iterable<Tree<DiffNode>> createSetDiffIterable(@Nonnull DiffNode parentDiffNode,
            @Nonnull Object src, @Nonnull Object target) {
        final Set<Object> srcSet = (Set<Object>) src;
        final Set<Object> targetSet = (Set<Object>) target;
        final Set<Object> allElementsSet = new HashSet<>(targetSet);
        allElementsSet.addAll(srcSet);
        List<Tree<DiffNode>> diffGroupNextLevelRootNodes = new ArrayList<>();

        int diffIndex = 0;
        for (Object obj : allElementsSet) {
            boolean srcHasElement = srcSet.contains(obj);
            boolean targetHasElement = targetSet.contains(obj);
            if (srcHasElement && targetHasElement) {
                //noinspection UnnecessaryContinue
                continue;
            } else {
                DiffNode nextLevelRoot;
                if (srcHasElement) {
                    nextLevelRoot = createDiffGroupOneLevel(obj, DELETION_MARK);
                } else {
                    nextLevelRoot = new DiffNode(new Diff(Diff.Operation.ADD_VALUE, null, obj));
                }
                diffGroupNextLevelRootNodes.add(new Tree<>(nextLevelRoot));
                parentDiffNode.addFieldDiff(diffIndex++, nextLevelRoot);
            }
        }

        return diffGroupNextLevelRootNodes;
    }

    @SuppressWarnings("unchecked")
    private Iterable<Tree<DiffNode>> createMapDiffIterable(@Nonnull DiffNode parentDiffNode,
            @Nonnull Object src, @Nonnull Object target) {
        final Map<Object, Object> srcMap = (Map<Object, Object>) src;
        final Map<Object, Object> targetMap = (Map<Object, Object>) target;
        final Map<Object, Object> mapWithAllElements = new HashMap<>(targetMap);
        mapWithAllElements.putAll(srcMap);

        List<Tree<DiffNode>> diffGroupNextLevelRootNodes = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : mapWithAllElements.entrySet()) {
            Object key = entry.getKey();
            boolean srcHasKey = srcMap.containsKey(key);
            boolean targetHasKey = targetMap.containsKey(key);
            if (srcHasKey && targetHasKey) {
                Object srcElement = srcMap.get(key);
                Object targetElement = targetMap.get(key);
                if (isEqualTo(srcElement, targetElement)) {
                    continue;
                }
                Tree<DiffNode> nextLevelTreeNode = createNextDiffTreeNode(srcElement, targetElement);
                diffGroupNextLevelRootNodes.add(nextLevelTreeNode);
                parentDiffNode.addFieldDiff(key, nextLevelTreeNode.getNodeValue());
            } else {
                DiffNode nextLevelRoot;
                if (srcHasKey) {
                    nextLevelRoot = createDiffGroupOneLevel(entry.getValue(), DELETION_MARK);
                } else {
                    nextLevelRoot = new DiffNode(new Diff(Diff.Operation.ADD_VALUE, null, entry.getValue()));
                }
                diffGroupNextLevelRootNodes.add(new Tree<>(nextLevelRoot));
                parentDiffNode.addFieldDiff(key, nextLevelRoot);
            }
        }

        return diffGroupNextLevelRootNodes;
    }

    private Tree<DiffNode> createNextDiffTreeNode(@Nullable Object src, @Nullable Object target) {
        DiffNode node;
        if (src == target) {
            node = new DiffNode(new Diff());
        } else if (src == null || target == null) {
            node = new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE, src, target));
        } else {
            node = diffUsingClassLevelCustomHandler(src, target);
        }
        if (node != null) {
            return new Tree<>(node);
        }
        node = createDiffGroupOneLevel(src, target);
        Iterable<Tree<DiffNode>> nextLevelDiffChildren =
                generateChildDiffGroups(node, src, target);
        return new Tree<>(node, nextLevelDiffChildren);
    }

    @SuppressWarnings("unchecked")
    private DiffNode diffUsingClassLevelCustomHandler(@Nonnull Object src, @Nonnull Object target) {
        Class<Object> clazz = (Class<Object>) determineClass(src, target);
        if (!isEnabled(Feature.DiffingHandler.IGNORE_CLASS_TYPE_HANDLER)) {
            TypeHandler typeHandler = ReflectionUtils.getClassAnnotation(clazz, TypeHandler.class);
            if (typeHandler != null) {
                Class<? extends DiffingHandler<?>> handlerClass = typeHandler.diffUsing();
                if (handlerClass != TypeHandler.None.class) {
                    DiffingHandler<Object> diffingHandler;
                    try {
                        diffingHandler = (DiffingHandler<Object>) handlerClass.newInstance();
                    }
                    catch (Throwable e) {
                        String message = String.format("Failed to instantiate diffing handler %s for %s",
                                handlerClass.getName(),
                                clazz.getName());
                        throw new DiffException(message);
                    }
                    return diffingHandler.diff(src, target, new DefaultDiffingContext(this));
                }
            }
        }
        if (!isEnabled(Feature.DiffingHandler.IGNORE_GLOBAL_TYPE_HANDLER)) {
            DiffingHandler<Object> diffingHandler = getDiffingHandler(clazz);
            if (diffingHandler != null) {
                return diffingHandler.diff(src, target, new DefaultDiffingContext(this));
            }
        }
        return null;
    }

    @Override
    public <T> T applyDiff(@Nullable T src, @Nonnull DiffNode diffs, @Nonnull Set<Feature.MergingStrategy> mergingStrategies) {
        DiffApplicationTree diffApplicationTree = new DiffApplicationTree(src, diffs);
        try {
            Iterable<DiffApplicationTree> applicationTreeNodes = diffApplicationTree.preOrderTraversal();
            Iterator<DiffApplicationTree> iter = applicationTreeNodes.iterator();
            @SuppressWarnings("unchecked")
            T newRoot = (T) iter.next().applyDiff(new DefaultMergingContext(
                    this,
                    mergingStrategies,
                    true
            ));
            while (iter.hasNext()) {
                DiffApplicationTree node = iter.next();
                node.applyDiff(new DefaultMergingContext(
                        this,
                        mergingStrategies,
                        false
                ));
            }
            return newRoot;
        }
        catch (Throwable e) {
            if (e instanceof MergingException) {
                throw e;
            }
            throw new MergingException("Failed to merge diff: " + e.getMessage(), e);
        }
    }

}
