package org.xcorpion.jdiff.util.collection;

import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffNode;
import org.xcorpion.jdiff.exception.DiffApplicationException;
import org.xcorpion.jdiff.util.reflection.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class DiffApplicationTree implements TreeLike<DiffApplicationTree> {

    private Object root;
    private DiffNode diffNode;

    public DiffApplicationTree(Object root, DiffNode diffNode) {
        this.root = root;
        this.diffNode = diffNode;
    }

    @Override
    public DiffApplicationTree getNodeValue() {
        return this;
    }

    public void applyDiff() {

    }

    @Override
    public Iterable<DiffApplicationTree> getChildren() {
        return () -> new Iterator<DiffApplicationTree>() {
            private Iterator<Map.Entry<Object, DiffNode>> diffIter = diffNode.getFieldDiffs() != null ?
                    diffNode.getFieldDiffs().entrySet().iterator() :
                    Collections.emptyIterator();
            int index = 0;
            Iterator<Object> collectionIter = null;

            @Override
            public boolean hasNext() {
                return diffIter.hasNext();
            }

            @SuppressWarnings("unchecked, ConstantConditions")
            @Override
            public DiffApplicationTree next() {
                if (root == null) {
                    throw new DiffApplicationException("Invalid state: attempting to apply child diff to null object");
                }
                Map.Entry<Object, DiffNode> diffEntry = diffIter.next();
                Object fieldKey = diffEntry.getKey();
                DiffNode diffNode = diffEntry.getValue();
                Diff diff = diffNode.getDiff();
                Diff.Operation diffOperation = diff.getOperation();
                if (root.getClass().isArray()) {
                    if (!(fieldKey instanceof Integer)) {
                        throw new DiffApplicationException("Expect index value for array object. Got: " + fieldKey);
                    }
                    Integer key = (Integer) fieldKey;
                    Object fieldObj = Array.get(root, key);
                    return new DiffApplicationTree(fieldObj, diffNode);
                } else if (root instanceof Iterable) {
                    if (collectionIter == null) {
                        collectionIter = ((Iterable<Object>) root).iterator();
                    }
                    Object fieldObj = collectionIter.next();
                    if (!(fieldKey instanceof Integer)) {
                        throw new DiffApplicationException("Expect index value for iterable object. Got: " + fieldKey);
                    }
                    Integer key = (Integer) fieldKey;
                    while (index != key) {
                        fieldObj = collectionIter.next();
                    }
                    return new DiffApplicationTree(fieldObj, diffNode);
                } else if (root instanceof Set) {
                    if (collectionIter == null) {
                        collectionIter = ((Set<Object>) root).iterator();
                    }
                    Object fieldObj = collectionIter.next();
                    return new DiffApplicationTree(fieldObj, diffNode);
                } else if (root instanceof Map) {
                    Map<Object, Object> map = (Map<Object, Object>) root;
                    Object fieldObj = map.get(fieldKey);
                    return new DiffApplicationTree(fieldObj, diffNode);
                }
                if (!(fieldKey instanceof Integer)) {
                    throw new DiffApplicationException("Expect field name. Got: " + fieldKey);
                }
                String fieldName = (String) fieldKey;
                Field field = ReflectionUtils.getField(root, fieldName);
                if (field == null) {
                    throw new DiffApplicationException("Unable to find " + fieldName +
                            " in class " + root.getClass().getName());
                }
                field.setAccessible(true);
                Object fieldObj;
                try {
                    fieldObj = field.get(root);
                } catch (IllegalAccessException e) {
                    throw new DiffApplicationException("Unable to access " + fieldName +
                            " in class " + root.getClass().getName(), e);
                }
                return new DiffApplicationTree(fieldObj, diffNode);
            }
        };
    }
}
