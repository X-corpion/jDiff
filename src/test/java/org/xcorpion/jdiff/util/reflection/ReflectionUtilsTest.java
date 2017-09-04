package org.xcorpion.jdiff.util.reflection;

import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ReflectionUtilsTest {

    private static class ObjectWithCollection {

        List<String> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();

    }

    private static class CloneableObjectWithCollection extends ObjectWithCollection implements Cloneable {

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    private static class ObjectWithImmutableCollection {

        Set<String> set;

    }

    private static class NestedObject {

        NestedObject nested;
        int level;

    }

    private static class MultipleEntryObject {

        NestedObject nested = new NestedObject();
        {
            nested.level = 10;
        }
        Object nullObject;
        int intPrimitive = 1;

    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface NonInheritedAnnotation {

    }

    @NonInheritedAnnotation
    private static class ClassWithNonInheritedAnnotation {

    }

    private static class ChildClassWithNonInheritedAnnotation extends ClassWithNonInheritedAnnotation {

    }

    @Test
    public void deepCloneShouldHandleCollections() {
        ObjectWithCollection obj = new ObjectWithCollection();
        obj.list.add("123");
        obj.map.put("456", "789");
        ObjectWithCollection cloned = ReflectionUtils.deepClone(obj);
        assertNotNull(cloned);
        assertThat(cloned.list, is(Collections.singletonList("123")));
    }

    @Test
    public void deepCloneShouldHandleImmutableCollections() {
        ObjectWithImmutableCollection obj = new ObjectWithImmutableCollection();
        obj.set = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("123", "456")));
        ObjectWithImmutableCollection cloned = ReflectionUtils.deepClone(obj);
        Set<String> expected = new HashSet<>(Arrays.asList("123", "456"));
        assertNotNull(cloned);
        assertThat(cloned.set, is(expected));
        assertThat(cloned.set.getClass().getCanonicalName(), is("java.util.Collections.UnmodifiableSet"));
    }

    @Test
    public void deepClonedObjectShouldHaveNothingToDoWithOriginalObject() {
        MultipleEntryObject src = new MultipleEntryObject();
        MultipleEntryObject cloned = ReflectionUtils.deepClone(src);
        src.nested.level = 1;
        src.nested = null;
        src.intPrimitive = 123;
        src.nullObject = "not null";

        assertNotNull(cloned);
        assertThat(cloned.nested.level, is(10));
        assertThat(cloned.intPrimitive, is(1));
        assertThat(cloned.nullObject, is(nullValue()));
    }

    @Test
    public void deepCloneShouldHandleNested() {
        NestedObject root = new NestedObject();
        root.level = 0;
        NestedObject current = root;
        for (int i = 0; i < 5000; i++) {
            current.nested = new NestedObject();
            current.nested.level = i + 1;
            current = current.nested;
        }
        NestedObject cloned = ReflectionUtils.deepClone(root);
        assertNotNull(cloned);
        for (int i = 0; i < 5000; i++) {
            assertNotNull(cloned.nested);
            assertThat(cloned.nested.level, is(i + 1));
            cloned = cloned.nested;
        }
    }

    @Test
    public void shallowCloneCloneableShouldNotCloneNestedObjects() {
        CloneableObjectWithCollection obj = new CloneableObjectWithCollection();
        obj.list = Arrays.asList("1", "2");
        obj.map = new HashMap<>();
        obj.map.put("1", "2");
        CloneableObjectWithCollection cloned = ReflectionUtils.shallowClone(obj);
        assertNotNull(cloned);
        assertThat(cloned == obj, is(false));
        assertThat(cloned.list == obj.list, is(true));
        assertThat(cloned.map == obj.map, is(true));
    }

    @Test
    public void shallowCloneRegularClassShouldNotCloneNestedObjects() {
        ObjectWithCollection obj = new ObjectWithCollection();
        obj.list = Arrays.asList("1", "2");
        obj.map = new HashMap<>();
        obj.map.put("1", "2");
        ObjectWithCollection cloned = ReflectionUtils.shallowClone(obj);
        assertNotNull(cloned);
        assertThat(cloned == obj, is(false));
        assertThat(cloned.list == obj.list, is(true));
        assertThat(cloned.map == obj.map, is(true));
    }

    @Test
    public void getAnnotationOnClassShouldReturnNullIfObjectIsNull() {
        assertThat(ReflectionUtils.getClassAnnotation((Object) null, Override.class), is(nullValue()));
    }

    @Test
    public void getAnnotationOnClassShouldFindNonInheritedAnnotationInSuperclass() {
        ChildClassWithNonInheritedAnnotation obj = new ChildClassWithNonInheritedAnnotation();
        NonInheritedAnnotation anno = ReflectionUtils.getClassAnnotation(obj, NonInheritedAnnotation.class);
        assertThat(anno, is(notNullValue()));
    }



}
