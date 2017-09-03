package org.xcorpion.jdiff.util.reflection;

import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ReflectionUtilsTest {

    private static class ObjectWithCollection {

        List<String> list = new ArrayList<String>();
        Map<String, String> map = new HashMap<String, String>();

    }

    private static class ObjectWithImmutableCollection {

        Set<String> set;

    }

    private static class NestedObject {

        public NestedObject nested;
        public int level;

    }

    private static class MultipleEntryObject {

        public NestedObject nested = new NestedObject();
        {
            nested.level = 10;
        }
        public Object nullObject;
        public int intPrimitive = 1;

    }

    @Test
    public void deepCloneShouldHandleCollections() {
        ObjectWithCollection obj = new ObjectWithCollection();
        obj.list.add("123");
        obj.map.put("456", "789");
        ObjectWithCollection cloned = ReflectionUtils.deepClone(obj);
        assertThat(cloned.list, is(Collections.singletonList("123")));
    }

    @Test
    public void deepCloneShouldHandleImmutableCollections() {
        ObjectWithImmutableCollection obj = new ObjectWithImmutableCollection();
        obj.set = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("123", "456")));
        ObjectWithImmutableCollection cloned = ReflectionUtils.deepClone(obj);
        Set<String> expected = new HashSet<String>(Arrays.asList("123", "456"));
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
        for (int i = 0; i < 5000; i++) {
            assertNotNull(cloned.nested);
            assertThat(cloned.nested.level, is(i + 1));
            cloned = cloned.nested;
        }
    }

}