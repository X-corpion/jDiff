package org.xcorpion.jdiff.util;

import org.junit.Before;
import org.junit.Test;
import org.xcorpion.jdiff.api.*;
import org.xcorpion.jdiff.exception.MergingException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
public class ReflectionObjectDiffMapperTest {

    private ReflectionObjectDiffMapper diffMapper;

    private static class TestClass implements Serializable {

        Object field1;
        Object field2;

        public TestClass(Object field1, Object field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestClass testClass = (TestClass) o;

            if (field1 != null ? !field1.equals(testClass.field1) : testClass.field1 != null) return false;
            return field2 != null ? field2.equals(testClass.field2) : testClass.field2 == null;
        }

        @Override
        public int hashCode() {
            int result = field1 != null ? field1.hashCode() : 0;
            result = 31 * result + (field2 != null ? field2.hashCode() : 0);
            return result;
        }
    }

    private static class TestClassWithTransientFields {

        Object field1;
        transient Object field2;

        public TestClassWithTransientFields(Object field1, Object field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }

    private static class TestClassWithBrokenEqualityCheck {
        Object field;

        public TestClassWithBrokenEqualityCheck(Object field) {
            this.field = field;
        }

        @Override
        public boolean equals(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return field != null ? field.hashCode() : 0;
        }

    }

    @Before
    public void setUp() {
        diffMapper = new ReflectionObjectDiffMapper();
    }

    @Test
    public void diffTwoIntegers() {
        DiffNode diffNode = diffMapper.diff(1, 3);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(1));
        assertThat(diffNode.getDiff().getTargetValue(), is(3));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Test
    public void diffTwoBooleans() {
        DiffNode diffNode = diffMapper.diff(true, false);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(true));
        assertThat(diffNode.getDiff().getTargetValue(), is(false));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Test
    public void diffTwoStrings() {
        DiffNode diffNode = diffMapper.diff("abc", "def");

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is("abc"));
        assertThat(diffNode.getDiff().getTargetValue(), is("def"));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Test
    public void diffIntegerAgainstNull() {
        DiffNode diffNode = diffMapper.diff(null, 3);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(3));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Test
    public void diffBooleanAgainstNull() {
        DiffNode diffNode = diffMapper.diff(null, true);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(true));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Test
    public void diffTwoBoxedBooleans() {
        DiffNode diffNode = diffMapper.diff(Boolean.TRUE, Boolean.FALSE);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(Boolean.TRUE));
        assertThat(diffNode.getDiff().getTargetValue(), is(Boolean.FALSE));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Test
    public void diffObjectAgainstNull() {
        TestClass obj1 = new TestClass("1", 2);
        DiffNode diffNode = diffMapper.diff(null, obj1);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(obj1));
    }

    @Test
    public void diffTwoSimpleObjects() {
        TestClass obj1 = new TestClass("1", "2");
        TestClass obj2 = new TestClass("1", null);
        DiffNode diffNode = diffMapper.diff(obj1, obj2);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        assertThat(diffNode.getFieldDiffs().entrySet(), hasSize(1));
        Diff diff = diffNode.getFieldDiffs().get("field2").getDiff();
        assertThat(diff.getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diff.getSrcValue(),is("2"));
        assertThat(diff.getTargetValue(), is(nullValue()));
    }

    @Test
    public void diffTwoObjectsWithNestedArrays() {
        TestClass obj1 = new TestClass("1", new int[] {1, 2, 3});
        TestClass obj2 = new TestClass("1", new int[] {2, 4});
        DiffNode diffNode = diffMapper.diff(obj1, obj2);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        assertThat(diffNode.getFieldDiffs().entrySet(), hasSize(1));
        DiffNode arrayDiffNode = diffNode.getFieldDiffs().get("field2");
        Map<Object, DiffNode> arrayDiffs = arrayDiffNode.getFieldDiffs();
        {
            Diff diff = arrayDiffNode.getDiff();
            assertThat(diff.getOperation(), is(Diff.Operation.RESIZE_ARRAY));
            assertThat(diff.getSrcValue(), is(3));
            assertThat(diff.getTargetValue(), is(2));
        }

        {
            DiffNode update0 = arrayDiffs.get(0);
            Diff diff = update0.getDiff();
            assertThat(diff.getOperation(), is(Diff.Operation.UPDATE_VALUE));
            assertThat(diff.getSrcValue(),is(1));
            assertThat(diff.getTargetValue(), is(2));
        }

        {
            DiffNode update1 = arrayDiffs.get(1);
            Diff diff = update1.getDiff();
            assertThat(diff.getOperation(), is(Diff.Operation.UPDATE_VALUE));
            assertThat(diff.getSrcValue(),is(2));
            assertThat(diff.getTargetValue(), is(4));
        }

        {
            DiffNode delete = arrayDiffs.get(2);
            Diff diff = delete.getDiff();
            assertThat(diff.getOperation(), is(Diff.Operation.REMOVE_VALUE));
            assertThat(diff.getSrcValue(),is(3));
            assertThat(diff.getTargetValue(), is(nullValue()));
        }

    }

    @Test
    public void canIgnoreTransientFields() {
        diffMapper.enable(Feature.IgnoreFields.TRANSIENT);

        TestClassWithTransientFields obj1 = new TestClassWithTransientFields("1", "2");
        TestClassWithTransientFields obj2 = new TestClassWithTransientFields("1", "3");
        DiffNode diffNode = diffMapper.diff(obj1, obj2);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Test
    public void returnsNoDiffIfObjectsAreEqual() {
        TestClass obj1 = new TestClass("1", 2);
        TestClass obj2 = new TestClass("1", 2);
        DiffNode diffNode = diffMapper.diff(obj1, obj2);

        assertThat(diffNode.getDiff(), is(nullValue()));
    }

    @Test
    public void isAbleToUseHashCodeForFastEqualityCheck() {
        diffMapper.enable(Feature.EqualityCheck.USE_HASHCODE);

        TestClassWithBrokenEqualityCheck obj1 = new TestClassWithBrokenEqualityCheck(1);
        TestClassWithBrokenEqualityCheck obj2 = new TestClassWithBrokenEqualityCheck(1);
        DiffNode diffNode = diffMapper.diff(obj1, obj2);

        assertThat(diffNode.getDiff(), is(nullValue()));
    }

    @Test
    public void diffTwoMapsWithBaseType() {
        Map<String, Integer> m1 = new TreeMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new TreeMap<>();
        m2.put("b", 1);
        m2.put("c", 2);
        DiffNode diffNode = diffMapper.diff(m1, m2);
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.entrySet(), hasSize(3));

        DiffNode removeValueDiff = fieldDiffs.get("a");
        assertThat(removeValueDiff.getDiff().getOperation(), is(Diff.Operation.REMOVE_VALUE));
        assertThat(removeValueDiff.getDiff().getSrcValue(), is(1));
        assertThat(removeValueDiff.getDiff().getTargetValue(), is(nullValue()));

        DiffNode updateValueDiff = fieldDiffs.get("b");
        assertThat(updateValueDiff.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(updateValueDiff.getDiff().getSrcValue(), is(2));
        assertThat(updateValueDiff.getDiff().getTargetValue(), is(1));

        DiffNode addValueDiff = fieldDiffs.get("c");
        assertThat(addValueDiff.getDiff().getOperation(), is(Diff.Operation.ADD_VALUE));
        assertThat(addValueDiff.getDiff().getSrcValue(), is(nullValue()));
        assertThat(addValueDiff.getDiff().getTargetValue(), is(2));
    }

    @Test
    public void diffTwoMapsWithNullKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put(null, 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put(null, 2);
        m2.put("b", 2);

        DiffNode diffNode = diffMapper.diff(m1, m2);
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.entrySet(), hasSize(1));

        DiffNode updateDiff = fieldDiffs.get(null);
        assertThat(updateDiff.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(updateDiff.getDiff().getSrcValue(), is(1));
        assertThat(updateDiff.getDiff().getTargetValue(), is(2));
    }

    @Test
    public void diffALargerMapWithASmallerOne() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put(null, 1);
        m1.put("b", 2);
        m1.put("c", 3);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put(null, 1);
        m2.put("b", 2);

        DiffNode diffNode = diffMapper.diff(m1, m2);
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.entrySet(), hasSize(1));

        DiffNode removalDiff = fieldDiffs.get("c");
        assertThat(removalDiff.getDiff().getOperation(), is(Diff.Operation.REMOVE_VALUE));
        assertThat(removalDiff.getDiff().getSrcValue(), is(3));
        assertThat(removalDiff.getDiff().getTargetValue(), is(nullValue()));
    }

    @Test
    public void diffASmallerMapWithALargerOne() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("b", 2);
        m2.put("c", 3);
        m2.put("d", 4);

        DiffNode diffNode = diffMapper.diff(m1, m2);
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.entrySet(), hasSize(2));

        DiffNode diff1 = fieldDiffs.get("c");
        assertThat(diff1.getDiff().getOperation(), is(Diff.Operation.ADD_VALUE));
        assertThat(diff1.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diff1.getDiff().getTargetValue(), is(3));

        DiffNode diff2 = fieldDiffs.get("d");
        assertThat(diff2.getDiff().getOperation(), is(Diff.Operation.ADD_VALUE));
        assertThat(diff2.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diff2.getDiff().getTargetValue(), is(4));
    }
    
    @Test
    public void diffMapsWithNonPrimitiveKeys() {
        Map<TestClass, Integer> m1 = new HashMap<>();
        m1.put(new TestClass("1", "2"), 2);
        Map<TestClass, Integer> m2 = new HashMap<>();
        m2.put(new TestClass("1", "2"), 2);
        m2.put(new TestClass("2", "3"), 3);

        DiffNode diffNode = diffMapper.diff(m1, m2);
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.entrySet(), hasSize(1));

        DiffNode diff1 = fieldDiffs.get(new TestClass("2", "3"));
        assertThat(diff1.getDiff().getOperation(), is(Diff.Operation.ADD_VALUE));
        assertThat(diff1.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diff1.getDiff().getTargetValue(), is(3));
    }

    @Test
    public void diffTwoSetsWithBaseType() {
        Set<String> s1 = new TreeSet<>(Arrays.asList("1", "2"));
        Set<String> s2 = new TreeSet<>(Arrays.asList("2", "3"));
        DiffNode diffNode = diffMapper.diff(s1, s2);
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.entrySet(), hasSize(2));
        assertThat(fieldDiffs.get(0).getDiff().getOperation(), is(Diff.Operation.REMOVE_VALUE));
        assertThat(fieldDiffs.get(0).getDiff().getSrcValue(), is("1"));
        assertThat(fieldDiffs.get(0).getDiff().getTargetValue(), is(nullValue()));

        assertThat(fieldDiffs.get(1).getDiff().getOperation(), is(Diff.Operation.ADD_VALUE));
        assertThat(fieldDiffs.get(1).getDiff().getSrcValue(), is(nullValue()));
        assertThat(fieldDiffs.get(1).getDiff().getTargetValue(), is("3"));
    }

    @Test
    public void diffTwoBooleanPrimitiveArrays() {
        DiffNode diffNode = diffMapper.diff(new boolean[] {true, false}, new boolean[] {false, false, true});
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.RESIZE_ARRAY));
        assertThat(diffNode.getDiff().getSrcValue(), is(2));
        assertThat(diffNode.getDiff().getTargetValue(), is(3));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.get(0).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(0).getDiff().getSrcValue(), is(true));
        assertThat(fieldDiffs.get(0).getDiff().getTargetValue(), is(false));
    }

    @Test
    public void diffTwoStringLists() {
        DiffNode diffNode = diffMapper.diff(Arrays.asList("a", "b"), Arrays.asList("a", "d"));
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.size(), is(1));
        assertThat(fieldDiffs.get(1).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(1).getDiff().getSrcValue(), is("b"));
        assertThat(fieldDiffs.get(1).getDiff().getTargetValue(), is("d"));
    }

    @Test
    public void diffTwoStringLinkedLists() {
        DiffNode diffNode = diffMapper.diff(
                new LinkedList<>(Arrays.asList("b", "d")),
                new LinkedList<>(Arrays.asList("a", "d"))
        );
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.size(), is(1));
        assertThat(fieldDiffs.get(0).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(0).getDiff().getSrcValue(), is("b"));
        assertThat(fieldDiffs.get(0).getDiff().getTargetValue(), is("a"));
    }

    @Test
    public void diffTwoListsWithNullValues() {
        DiffNode diffNode = diffMapper.diff(
                Arrays.asList(null, "b"),
                Arrays.asList("a", null)
        );
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.size(), is(2));

        assertThat(fieldDiffs.get(0).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(0).getDiff().getSrcValue(), is(nullValue()));
        assertThat(fieldDiffs.get(0).getDiff().getTargetValue(), is("a"));

        assertThat(fieldDiffs.get(1).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(1).getDiff().getSrcValue(), is("b"));
        assertThat(fieldDiffs.get(1).getDiff().getTargetValue(), is(nullValue()));
    }

    @Test
    public void diffASmallerListWithALargerOne() {
        DiffNode diffNode = diffMapper.diff(
                Arrays.asList("a", "b"),
                Arrays.asList("b", "b", "c")
        );
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.size(), is(2));

        assertThat(fieldDiffs.get(0).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(0).getDiff().getSrcValue(), is("a"));
        assertThat(fieldDiffs.get(0).getDiff().getTargetValue(), is("b"));

        assertThat(fieldDiffs.get(2).getDiff().getOperation(), is(Diff.Operation.ADD_VALUE));
        assertThat(fieldDiffs.get(2).getDiff().getSrcValue(), is(nullValue()));
        assertThat(fieldDiffs.get(2).getDiff().getTargetValue(), is("c"));
    }

    @Test
    public void diffALargerListWithASmallerOne() {
        DiffNode diffNode = diffMapper.diff(
                Arrays.asList("a", "b", "c"),
                Arrays.asList("b", "b")
        );
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.size(), is(2));

        assertThat(fieldDiffs.get(0).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(0).getDiff().getSrcValue(), is("a"));
        assertThat(fieldDiffs.get(0).getDiff().getTargetValue(), is("b"));

        assertThat(fieldDiffs.get(2).getDiff().getOperation(), is(Diff.Operation.REMOVE_VALUE));
        assertThat(fieldDiffs.get(2).getDiff().getSrcValue(), is("c"));
        assertThat(fieldDiffs.get(2).getDiff().getTargetValue(), is(nullValue()));
    }

    @Test
    public void diffTwoDeepObjectsThatWillFailRecursiveSolution() {
        // general equality will cause stack overflow error so we need to use the type handler feature
        diffMapper.registerTypeHandler(TestClass.class, new TypeHandler<TestClass>() {
            @Nonnull
            @Override
            public String getTypeId() {
                return TestClass.class.getName();
            }

            @Override
            public boolean isEqualTo(@Nullable TestClass src, @Nullable TestClass target) {
                return false;
            }
        });
        TestClass rootObj1 = new TestClass(null, null);
        {
            TestClass currentObj = rootObj1;
            for (int i = 0; i < 4999; i++) {
                TestClass obj = new TestClass(null, i + 1);
                currentObj.field1 = obj;
                currentObj.field2 = i;
                currentObj = obj;
            }
        }

        TestClass rootObj2 = new TestClass(null, null);
        {
            TestClass currentObj = rootObj2;
            for (int i = 0; i < 4999; i++) {
                TestClass obj = new TestClass(null, i + 2);
                currentObj.field1 = obj;
                currentObj.field2 = i + 1;
                currentObj = obj;
            }
        }

        DiffNode diffNode = diffMapper.diff(rootObj1, rootObj2);
        for (int i = 0; i < 5000; i++) {
            assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
            Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
            DiffNode field2Diff = fieldDiffs.get("field2");
            assertThat(field2Diff.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
            assertThat(field2Diff.getDiff().getSrcValue(), is(i));
            assertThat(field2Diff.getDiff().getTargetValue(), is(i + 1));
            diffNode = fieldDiffs.get("field1");
        }
    }

    @Test
    public void applyDiffFromNullToPrimitive() {
        DiffNode diff = diffMapper.diff(null, 100);
        int result = diffMapper.applyDiff(null, diff);
        assertThat(result, is(100));
    }

    @Test
    public void applyPrimitiveDiffFromOneValueToAnother() {
        DiffNode diff = diffMapper.diff(1, 100);
        int result = diffMapper.applyDiff(1, diff);
        assertThat(result, is(100));
    }

    @Test(expected = MergingException.class)
    public void applyPrimitiveDiffToAWrongSrcShouldThrowException() {
        DiffNode diff = diffMapper.diff(1, 100);
        diffMapper.applyDiff(true, diff);
    }

    @Test
    public void applyPrimitiveDiffToAnUnequalButCompatibleSourceShouldNotThrowException() {
        DiffNode diff = diffMapper.diff(1, 100);
        Object result = diffMapper.applyDiff(new Object(), diff);
        assertThat(result, is(100));
    }

    @Test
    public void applyArrayDiffWithElementsRemoved() {
        int[] src = new int[]{1, 2, 3};
        DiffNode diff = diffMapper.diff(src, new int[]{1});
        int[] result = diffMapper.applyDiff(src, diff);
        assertThat(result.length, is(1));
        assertThat(result[0], is(1));
    }

    @Test
    public void applyArrayDiffWithElementsAdded() {
        int[] src = new int[]{1};
        DiffNode diff = diffMapper.diff(src, new int[]{1, 2, 3});
        int[] result = diffMapper.applyDiff(src, diff);
        assertThat(result.length, is(3));
        assertThat(result[0], is(1));
        assertThat(result[1], is(2));
        assertThat(result[2], is(3));
    }

    @Test
    public void applyObjectDiff() {
        TestClass src = new TestClass("1", 2);
        TestClass target = new TestClass("2", 3);
        DiffNode diff = diffMapper.diff(src, target);
        TestClass result = diffMapper.applyDiff(src, diff);

        assertThat(src == result, is(true));
        assertThat(result.field1, is("2"));
        assertThat(result.field2, is(3));
    }

    @Test
    public void applyObjectDiffOntoNewObject() {
        TestClass src = new TestClass("1", 2);
        TestClass target = new TestClass("2", 3);
        DiffNode diff = diffMapper.diff(src, target);
        TestClass result = diffMapper.applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.CLONE_SOURCE_ROOT));

        assertThat(result == src, is(false));
        assertThat(result.field1, is("2"));
        assertThat(result.field2, is(3));

        assertThat(src.field1, is("1"));
        assertThat(src.field2, is(2));
    }

    @Test
    public void applyArrayDiffWithValueUpdateOnly() {
        DiffNode diff = diffMapper.diff(new int[] {1, 2}, new int[] {2, 2});
        int[] result = diffMapper.applyDiff(new int[]{1, 2}, diff);
        assertThat(result.length, is(2));
        assertThat(result[0], is(2));
        assertThat(result[1], is(2));
    }

    @Test
    public void applySetDiff() {
        Set<String> src = new HashSet<>();
        src.add("a");
        src.add("b");

        Set<String> target = new HashSet<>();
        target.add("a");
        target.add("c");

        DiffNode diff = diffMapper.diff(src, target);
        Set<String> result = diffMapper.applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.CLONE_SOURCE_ROOT));
        assertThat(src != target, is(true));
        assertThat(result.size(), is(2));
        assertThat(result, containsInAnyOrder("a", "c"));
        assertThat(result, not(contains("b")));
    }

    @Test
    public void applyMapDiffWithSimpleKey() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);

        Map<String, Integer> target = new HashMap<>();
        target.put("a", 2);
        target.put("c", 2);

        DiffNode diff = diffMapper.diff(src, target);
        Map<String, Integer> result = diffMapper.applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.CLONE_SOURCE_ROOT));
        assertThat(src != target, is(true));
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry("a", 2));
        assertThat(result, not(hasKey("b")));
        assertThat(result, hasEntry("c", 2));
    }

    @Test
    public void applyMapDiffWithObjectKey() {
        Map<TestClass, Integer> src = new HashMap<>();
        src.put(new TestClass(1, "a"), 1);
        src.put(new TestClass(2, "b"), 2);

        Map<TestClass, Integer> target = new HashMap<>();
        target.put(new TestClass(1, "a"), 2);
        target.put(new TestClass(2, "c"), 1);

        DiffNode diff = diffMapper.diff(src, target);
        Map<TestClass, Integer> result = diffMapper.applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.CLONE_SOURCE_ROOT));
        assertThat(src != target, is(true));
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry(new TestClass(1, "a"), 2));
        assertThat(result, not(hasKey(new TestClass(2, "b"))));
        assertThat(result, hasEntry(new TestClass(2, "c"), 1));
    }

}
