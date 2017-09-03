package org.xcorpion.jdiff.util.reflection;

import org.apache.commons.lang3.SerializationUtils;
import org.xcorpion.jdiff.exception.CloneException;
import org.xcorpion.jdiff.util.collection.Tree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ReflectionUtils {

    private static class FieldCloneMeta {

        final Object srcFieldValue;
        final Field field;
        final Object targetObject;

        FieldCloneMeta(Field field, Object srcFieldValue, Object targetObject) {
            this.field = field;
            this.srcFieldValue = srcFieldValue;
            this.targetObject = targetObject;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deepClone(@Nullable T srcObject) throws CloneException {
        if (srcObject == null) {
            return null;
        }
        if (srcObject.getClass().isPrimitive()) {
            // for primitives, cloning is unnecessary
            return srcObject;
        }
        if (srcObject instanceof Serializable) {
            return deepCloneSerializable((Serializable) srcObject);
        }

        T targetObject = generateEmptyCopy(srcObject);

        // lazily create an object field tree
        // then we do a post order traversal to populate the fields
        Tree<FieldCloneMeta> fieldsToCloneTree = new Tree<>(new FieldCloneMeta(null, srcObject, targetObject),
                populateFieldsToBeCloned(srcObject, targetObject));
        Iterable<FieldCloneMeta> fieldsToClone = fieldsToCloneTree.postOrderTraversal();
        for (FieldCloneMeta task : fieldsToClone) {
            Field f = task.field;
            if (f == null) {
                // root
                return targetObject;
            }
            f.setAccessible(true);
            Object srcValue = task.srcFieldValue;
            Object valueToSet = srcValue;
            if (srcValue instanceof Serializable) {
                valueToSet = deepCloneSerializable((Serializable) srcValue);
            }
            // otherwise there are two cases:
            // 1. valueToSet is null or primitive
            // 2. valueToSet was originally an empty copy but now its values are already set
            //    due to how traversing is done
            try {
                f.set(task.targetObject, valueToSet);
            } catch (IllegalAccessException e) {
                throw new CloneException("Failed to set field " + f.getName() + " in " +
                        getClassName(targetObject), e);
            }
        }
        return targetObject;
    }

    private static Iterable<Tree<FieldCloneMeta>> populateFieldsToBeCloned(final Object srcObject, final Object targetObject) {
        final List<Field> fields = getAllFieldsRecursive(srcObject.getClass());
        return () -> new Iterator<Tree<FieldCloneMeta>>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < fields.size();
            }

            @Override
            public Tree<FieldCloneMeta> next() {
                Field field = fields.get(currentIndex++);
                field.setAccessible(true);
                Object srcFieldValue;
                try {
                    srcFieldValue = field.get(srcObject);
                } catch (IllegalAccessException e) {
                    throw new CloneException("Failed to access field: " + field.getName() +
                            " of object " + getClassName(srcObject), e);
                }

                if (!requireManualDeepCopy(srcFieldValue)) {
                    return new Tree<>(
                            new FieldCloneMeta(field, srcFieldValue, targetObject));
                }

                // for every object that requires manual deep copy, create an new copy of its empty version.
                // later as post order traversal pops, its field values will be automatically filled.
                Object newHolderObject = generateEmptyCopy(srcFieldValue);

                return new Tree<>(new FieldCloneMeta(field, newHolderObject, targetObject),
                        populateFieldsToBeCloned(srcFieldValue, newHolderObject));
            }

        };
    }

    private static boolean requireManualDeepCopy(Object value) {
        return value != null &&
                !value.getClass().isPrimitive() &&
                !(value instanceof Serializable);
    }

    @SuppressWarnings("unchecked")
    private static <T> T generateEmptyCopy(T instance) {
        try {
            Constructor<T> defaultCtor = (Constructor<T>) instance.getClass().getDeclaredConstructor();
            defaultCtor.setAccessible(true);
            return defaultCtor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new CloneException(
                    "Failed to clone " + getClassName(instance) +
                            ": it should either extend Serializable or have a default constructor", e);
        } catch (IllegalAccessException e) {
            throw new CloneException("Failed to invoke default constructor on " + getClassName(instance), e);
        } catch (InstantiationException e) {
            throw new CloneException("Failed to create an empty object of " + getClassName(instance), e);
        } catch (InvocationTargetException e) {
            throw new CloneException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T deepCloneSerializable(Serializable instance) {
        return (T) SerializationUtils.clone(instance);
    }

    public static List<Field> getAllFieldsRecursive(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            Field[] fielz = clazz.getDeclaredFields();
            Collections.addAll(fields, fielz);
            clazz = clazz.getSuperclass();
            if (Object.class.equals(clazz)) {
                break;
            }
        }
        return fields;
    }

    public static Field getField(@Nonnull Object obj, @Nonnull String name) {
        Class<?> clz = obj.getClass();
        while (clz != null) {
            Field[] fields = obj.getClass().getDeclaredFields();
            Field field = null;
            for (Field f : fields) {
                if (f.getName().equals(name)) {
                    field = f;
                }
            }
            if (field == null) {
                clz = clz.getSuperclass();
                continue;
            }
            return field;
        }
        return null;
    }

    private static String getClassName(Object instance) {
        return instance.getClass().getName();
    }
}
