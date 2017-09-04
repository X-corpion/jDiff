package org.xcorpion.jdiff.testsuite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;
import org.xcorpion.jdiff.annotation.TypeHandler;
import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffNode;
import org.xcorpion.jdiff.api.Feature;
import org.xcorpion.jdiff.api.ObjectDiffMapper;
import org.xcorpion.jdiff.exception.MergingException;
import org.xcorpion.jdiff.handler.DateDiffingHandler;
import org.xcorpion.jdiff.handler.DateMergingHandler;
import org.xcorpion.jdiff.handler.TestIterableMergingHandler;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public abstract class ObjectDiffMapperTestSuite implements
        PrimitiveTestCases,
        ArrayTestCases,
        ListTestCases,
        IterableTestCases,
        SetTestCases,
        MapTestCases,
        ObjectTestCases,
        ExceptionTestCases,
        LoadTestCases,
        FeatureTestCases
{

    private static class TestClass implements Serializable {

        Object field1;
        Object field2;

        TestClass(Object field1, Object field2) {
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

        TestClassWithTransientFields(Object field1, Object field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }

    private static class TestClassWithBrokenEqualityCheck {
        Object field;

        TestClassWithBrokenEqualityCheck(Object field) {
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

    private static class TestClassWithIterable {

        @TypeHandler(
                mergeUsing = TestIterableMergingHandler.class
        )
        Iterable<String> stringIterable;

        TestClassWithIterable(Iterable<String> stringIterable) {
            this.stringIterable = stringIterable;
        }
    }

    private static class TestClassWithFieldTypeHandler {

        @TypeHandler(
                diffUsing = DateDiffingHandler.class,
                mergeUsing = DateMergingHandler.class
        )
        Date date;

        TestClassWithFieldTypeHandler(Date date) {
            this.date = date;
        }
    }

    protected abstract ObjectDiffMapper getDiffMapper();

    //region Primitive test cases
    @Override
    @Test
    public void diffTwoPrimitiveIntegers() {
        DiffNode diffNode = getDiffMapper().diff(1, 3);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(1));
        assertThat(diffNode.getDiff().getTargetValue(), is(3));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @SuppressWarnings("UnnecessaryBoxing")
    public void diffTwoBoxedIntegers() {
        DiffNode diffNode = getDiffMapper().diff(Integer.valueOf(1), Integer.valueOf(3));

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(1));
        assertThat(diffNode.getDiff().getTargetValue(), is(3));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @Test
    public void diffTwoPrimitiveBooleans() {
        DiffNode diffNode = getDiffMapper().diff(true, false);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(true));
        assertThat(diffNode.getDiff().getTargetValue(), is(false));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @SuppressWarnings("UnnecessaryBoxing")
    @Test
    public void diffTwoBoxedBooleans() {
        DiffNode diffNode = getDiffMapper().diff(Boolean.valueOf(true), Boolean.valueOf(false));

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(true));
        assertThat(diffNode.getDiff().getTargetValue(), is(false));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @Test
    public void diffTwoStrings() {
        DiffNode diffNode = getDiffMapper().diff("abc", "def");

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is("abc"));
        assertThat(diffNode.getDiff().getTargetValue(), is("def"));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @Test
    public void diffTwoNulls() {
        DiffNode diffNode = getDiffMapper().diff(null, null);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @Test
    public void diffIntegerAgainstNull() {
        DiffNode diffNode = getDiffMapper().diff(null, 3);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(3));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @Test
    public void diffNullAgainstInteger() {
        DiffNode diffNode = getDiffMapper().diff(1, null);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(1));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @Test
    public void diffBooleanAgainstNull() {
        DiffNode diffNode = getDiffMapper().diff(null, true);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(true));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }
    //endregion
    
    //region Array test cases
    @Override
    @Test
    public void diffTwoBooleanPrimitiveArrays() {
        DiffNode diffNode = getDiffMapper().diff(new boolean[]{true, false}, new boolean[]{false, false, true});
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.RESIZE_ARRAY));
        assertThat(diffNode.getDiff().getSrcValue(), is(2));
        assertThat(diffNode.getDiff().getTargetValue(), is(3));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.get(0).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(0).getDiff().getSrcValue(), is(true));
        assertThat(fieldDiffs.get(0).getDiff().getTargetValue(), is(false));
    }

    @Override
    @Test
    public void applyDiffFromNullToPrimitive() {
        DiffNode diff = getDiffMapper().diff(null, 100);
        int result = getDiffMapper().applyDiff(null, diff);
        assertThat(result, is(100));
    }

    @Override
    @Test
    public void applyPrimitiveDiffFromOneValueToAnother() {
        DiffNode diff = getDiffMapper().diff(1, 100);
        int result = getDiffMapper().applyDiff(1, diff);
        assertThat(result, is(100));
    }

    @Override
    @Test(expected = MergingException.class)
    public void applyPrimitiveDiffToAWrongSrcShouldThrowException() {
        DiffNode diff = getDiffMapper().diff(1, 100);
        getDiffMapper().applyDiff(true, diff);
    }

    @Override
    @Test
    public void applyPrimitiveDiffToAnUnequalButCompatibleSourceShouldNotThrowException() {
        DiffNode diff = getDiffMapper().diff(1, 100);
        Object result = getDiffMapper().applyDiff(new Object(), diff);
        assertThat(result, is(100));
    }

    @Override
    @Test
    public void applyArrayDiffWithElementsRemoved() {
        int[] src = new int[]{1, 2, 3};
        DiffNode diff = getDiffMapper().diff(src, new int[]{1});
        int[] result = getDiffMapper().applyDiff(src, diff);
        assertThat(result.length, is(1));
        assertThat(result[0], is(1));
    }

    @Override
    @Test
    public void applyArrayDiffWithElementsAdded() {
        int[] src = new int[]{1};
        DiffNode diff = getDiffMapper().diff(src, new int[]{1, 2, 3});
        int[] result = getDiffMapper().applyDiff(src, diff);
        assertThat(result.length, is(3));
        assertThat(result[0], is(1));
        assertThat(result[1], is(2));
        assertThat(result[2], is(3));
    }

    @Override
    @Test
    public void applyArrayDiffWithValueUpdateOnly() {
        DiffNode diff = getDiffMapper().diff(new int[]{1, 2}, new int[]{2, 2});
        int[] result = getDiffMapper().applyDiff(new int[]{1, 2}, diff);
        assertThat(result.length, is(2));
        assertThat(result[0], is(2));
        assertThat(result[1], is(2));
    }
    //endregion

    //region List test casts
    @Override
    @Test
    public void diffTwoStringLists() {
        DiffNode diffNode = getDiffMapper().diff(Arrays.asList("a", "b"), Arrays.asList("a", "d"));
        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));

        Map<Object, DiffNode> fieldDiffs = diffNode.getFieldDiffs();
        assertThat(fieldDiffs.size(), is(1));
        assertThat(fieldDiffs.get(1).getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(fieldDiffs.get(1).getDiff().getSrcValue(), is("b"));
        assertThat(fieldDiffs.get(1).getDiff().getTargetValue(), is("d"));
    }

    @Override
    @Test
    public void diffTwoStringLinkedLists() {
        DiffNode diffNode = getDiffMapper().diff(
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

    @Override
    @Test
    public void diffTwoListsWithNullValues() {
        DiffNode diffNode = getDiffMapper().diff(
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

    @Override
    @Test
    public void diffASmallerListWithALargerOne() {
        DiffNode diffNode = getDiffMapper().diff(
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

    @Override
    @Test
    public void diffALargerListWithASmallerOne() {
        DiffNode diffNode = getDiffMapper().diff(
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

    @Override
    @Test
    public void applyDiffToPrimitiveList() {
        List<Integer> src = new ArrayList<>();
        src.add(1);
        src.add(2);

        List<Integer> target = new ArrayList<>();
        target.add(2);
        target.add(3);

        DiffNode diff = getDiffMapper().diff(src, target);
        List<Integer> result = getDiffMapper().applyDiff(src, diff);

        assertThat(result, hasSize(2));
        assertThat(result.get(0), is(2));
        assertThat(result.get(1), is(3));
    }

    @Override
    @Test
    public void applyDiffToCustomObjectList() {
        List<TestClass> src = new ArrayList<>();
        src.add(new TestClass("a", 1));
        src.add(new TestClass("b", 2));

        List<TestClass> target = new ArrayList<>();
        target.add(new TestClass("b", 2));
        target.add(new TestClass("c", 3));
        target.add(new TestClass("d", 4));

        DiffNode diff = getDiffMapper().diff(src, target);
        List<TestClass> result = getDiffMapper().applyDiff(src, diff);

        assertThat(result, hasSize(3));
        assertThat(result.get(0), is(new TestClass("b", 2)));
        assertThat(result.get(1), is(new TestClass("c", 3)));
        assertThat(result.get(2), is(new TestClass("d", 4)));

        // merging will be done using existing objects but their values would be altered
        assertThat(result.get(0) != target.get(0), is(true));
        assertThat(result.get(1) != target.get(1), is(true));

        // this one will be using the target element directly by default as it doesn't exist in src
        assertThat(result.get(2) == target.get(2), is(true));
    }

    @Override
    @Test
    public void applyDiffToNestedCollectionList() {
        List<Set<Integer>> src = new ArrayList<>();
        Set<Integer> srcSet1 = new HashSet<>();
        srcSet1.add(1);
        srcSet1.add(2);
        Set<Integer> srcSet2 = new HashSet<>();
        srcSet2.add(3);
        src.add(srcSet1);
        src.add(srcSet2);


        List<Set<Integer>> target = new ArrayList<>();
        Set<Integer> targetSet1 = new HashSet<>();
        targetSet1.add(0);
        targetSet1.add(1);
        target.add(targetSet1);

        DiffNode diff = getDiffMapper().diff(src, target);
        List<Set<Integer>> result = getDiffMapper().applyDiff(src, diff);
        assertThat(result, hasSize(1));

        // the wrapped collection will be reused by default
        assertThat(result.get(0) == src.get(0), is(true));

        // however the values will be altered
        assertThat(result.get(0), containsInAnyOrder(0, 1));
        assertThat(result.get(0), not(contains(2)));
    }
    //endregion

    //region Iterable test cases

    @Override
    @Test(expected = MergingException.class)
    public void iterableMergingShouldThrowExceptionByDefault() {
        List<String> srcList = Arrays.asList("1", "2");
        List<String> targetList = Arrays.asList("1", "3", "4");
        TestClassWithIterable src = new TestClassWithIterable(() -> srcList.iterator());
        TestClassWithIterable target = new TestClassWithIterable(() -> targetList.iterator());
        ObjectDiffMapper diffMapper = getDiffMapper();
        diffMapper.enable(Feature.TypeHandler.IGNORE_FIELD_TYPE_HANDLER_FOR_MERGING);
        DiffNode diff = diffMapper.diff(src, target);
        diffMapper.applyDiff(src, diff);
    }

    @SuppressWarnings("Convert2MethodRef")
    @Override
    @Test
    public void iterableMergingCanBeDoneWithCustomMerger() {
        List<String> srcList = Arrays.asList("1", "2");
        List<String> targetList = Arrays.asList("1", "3", "4");
        TestClassWithIterable src = new TestClassWithIterable(() -> srcList.iterator());
        TestClassWithIterable target = new TestClassWithIterable(() -> targetList.iterator());
        ObjectDiffMapper diffMapper = getDiffMapper();
        DiffNode diff = diffMapper.diff(src, target);
        TestClassWithIterable result = diffMapper.applyDiff(src, diff);
        assertThat(result.stringIterable, containsInAnyOrder("1", "3", "4"));
        assertThat(result.stringIterable, not(contains("2")));
    }

    //endregion

    //region Map test cases
    @Override
    @Test
    public void diffTwoMapsWithBaseType() {
        Map<String, Integer> m1 = new TreeMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new TreeMap<>();
        m2.put("b", 1);
        m2.put("c", 2);
        DiffNode diffNode = getDiffMapper().diff(m1, m2);
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

    @Override
    @Test
    public void diffTwoMapsWithNullKey() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put(null, 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put(null, 2);
        m2.put("b", 2);

        DiffNode diffNode = getDiffMapper().diff(m1, m2);
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

    @Override
    @Test
    public void diffALargerMapWithASmallerOne() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put(null, 1);
        m1.put("b", 2);
        m1.put("c", 3);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put(null, 1);
        m2.put("b", 2);

        DiffNode diffNode = getDiffMapper().diff(m1, m2);
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

    @Override
    @Test
    public void diffASmallerMapWithALargerOne() {
        Map<String, Integer> m1 = new HashMap<>();
        m1.put("b", 2);
        Map<String, Integer> m2 = new HashMap<>();
        m2.put("b", 2);
        m2.put("c", 3);
        m2.put("d", 4);

        DiffNode diffNode = getDiffMapper().diff(m1, m2);
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

    @Override
    @Test
    public void diffMapsWithNonPrimitiveKeys() {
        Map<TestClass, Integer> m1 = new HashMap<>();
        m1.put(new TestClass("1", "2"), 2);
        Map<TestClass, Integer> m2 = new HashMap<>();
        m2.put(new TestClass("1", "2"), 2);
        m2.put(new TestClass("2", "3"), 3);

        DiffNode diffNode = getDiffMapper().diff(m1, m2);
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

    @Override
    @Test
    public void applyMapDiffWithSimpleKey() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);

        Map<String, Integer> target = new HashMap<>();
        target.put("a", 2);
        target.put("c", 2);

        DiffNode diff = getDiffMapper().diff(src, target);
        Map<String, Integer> result = getDiffMapper().applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.SHALLOW_CLONE_SOURCE_ROOT));
        assertThat(src != target, is(true));
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry("a", 2));
        assertThat(result, not(hasKey("b")));
        assertThat(result, hasEntry("c", 2));
    }

    @Override
    @Test
    public void applyMapDiffWithObjectKey() {
        Map<TestClass, Integer> src = new HashMap<>();
        src.put(new TestClass(1, "a"), 1);
        src.put(new TestClass(2, "b"), 2);

        Map<TestClass, Integer> target = new HashMap<>();
        target.put(new TestClass(1, "a"), 2);
        target.put(new TestClass(2, "c"), 1);

        DiffNode diff = getDiffMapper().diff(src, target);
        Map<TestClass, Integer> result = getDiffMapper().applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.SHALLOW_CLONE_SOURCE_ROOT));
        assertThat(src != target, is(true));
        assertThat(result.size(), is(2));
        assertThat(result, hasEntry(new TestClass(1, "a"), 2));
        assertThat(result, not(hasKey(new TestClass(2, "b"))));
        assertThat(result, hasEntry(new TestClass(2, "c"), 1));
    }

    //endregion

    //region Set test cases
    @Override
    @Test
    public void diffTwoSetsWithBaseType() {
        Set<String> s1 = new TreeSet<>(Arrays.asList("1", "2"));
        Set<String> s2 = new TreeSet<>(Arrays.asList("2", "3"));
        DiffNode diffNode = getDiffMapper().diff(s1, s2);
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

    @Override
    @Test
    public void applySetDiff() {
        Set<String> src = new HashSet<>();
        src.add("a");
        src.add("b");

        Set<String> target = new HashSet<>();
        target.add("a");
        target.add("c");

        DiffNode diff = getDiffMapper().diff(src, target);
        Set<String> result = getDiffMapper().applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.SHALLOW_CLONE_SOURCE_ROOT));
        assertThat(src != target, is(true));
        assertThat(result.size(), is(2));
        assertThat(result, containsInAnyOrder("a", "c"));
        assertThat(result, not(contains("b")));
    }
    //endregion

    //region Object test cases

    @Override
    @Test
    public void diffObjectAgainstNull() {
        TestClass obj1 = new TestClass("1", 2);
        DiffNode diffNode = getDiffMapper().diff(null, obj1);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(obj1));
    }

    @Override
    @Test
    public void diffTwoSimpleObjects() {
        TestClass obj1 = new TestClass("1", "2");
        TestClass obj2 = new TestClass("1", null);
        DiffNode diffNode = getDiffMapper().diff(obj1, obj2);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getDiff().getSrcValue(), is(nullValue()));
        assertThat(diffNode.getDiff().getTargetValue(), is(nullValue()));

        assertThat(diffNode.getFieldDiffs().entrySet(), hasSize(1));
        Diff diff = diffNode.getFieldDiffs().get("field2").getDiff();
        assertThat(diff.getOperation(), is(Diff.Operation.UPDATE_VALUE));
        assertThat(diff.getSrcValue(), is("2"));
        assertThat(diff.getTargetValue(), is(nullValue()));
    }

    @Override
    @Test
    public void diffTwoObjectsWithNestedArrays() {
        TestClass obj1 = new TestClass("1", new int[]{1, 2, 3});
        TestClass obj2 = new TestClass("1", new int[]{2, 4});
        DiffNode diffNode = getDiffMapper().diff(obj1, obj2);

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
            assertThat(diff.getSrcValue(), is(1));
            assertThat(diff.getTargetValue(), is(2));
        }

        {
            DiffNode update1 = arrayDiffs.get(1);
            Diff diff = update1.getDiff();
            assertThat(diff.getOperation(), is(Diff.Operation.UPDATE_VALUE));
            assertThat(diff.getSrcValue(), is(2));
            assertThat(diff.getTargetValue(), is(4));
        }

        {
            DiffNode delete = arrayDiffs.get(2);
            Diff diff = delete.getDiff();
            assertThat(diff.getOperation(), is(Diff.Operation.REMOVE_VALUE));
            assertThat(diff.getSrcValue(), is(3));
            assertThat(diff.getTargetValue(), is(nullValue()));
        }

    }

    @Override
    @Test
    public void returnsNoDiffIfObjectsAreEqual() {
        TestClass obj1 = new TestClass("1", 2);
        TestClass obj2 = new TestClass("1", 2);
        DiffNode diffNode = getDiffMapper().diff(obj1, obj2);

        assertThat(diffNode.isEmpty(), is(true));
    }

    @Override
    @Test
    public void applyObjectDiff() {
        TestClass src = new TestClass("1", 2);
        TestClass target = new TestClass("2", 3);
        DiffNode diff = getDiffMapper().diff(src, target);
        TestClass result = getDiffMapper().applyDiff(src, diff);

        assertThat(src == result, is(true));
        assertThat(result.field1, is("2"));
        assertThat(result.field2, is(3));
    }

    @Override
    @Test
    public void applyObjectDiffOntoNewObject() {
        TestClass src = new TestClass("1", 2);
        TestClass target = new TestClass("2", 3);
        DiffNode diff = getDiffMapper().diff(src, target);
        TestClass result = getDiffMapper().applyDiff(src, diff, Collections.singleton(Feature.MergingStrategy.SHALLOW_CLONE_SOURCE_ROOT));

        assertThat(result == src, is(false));
        assertThat(result.field1, is("2"));
        assertThat(result.field2, is(3));

        assertThat(src.field1, is("1"));
        assertThat(src.field2, is(2));
    }

    //endregion

    //region Load test cases
    @Override
    @Test
    public void diffTwoDeepObjectsThatWillFailRecursiveSolution() {
        // general equality will cause stack overflow error so we need to use the type handler feature
        getDiffMapper().registerEqualityChecker(TestClass.class, (src, target) -> false);
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

        DiffNode diffNode = getDiffMapper().diff(rootObj1, rootObj2);
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
    //endregion

    //region Feature test cases
    @Override
    @Test
    public void canIgnoreTransientFields() {
        getDiffMapper().enable(Feature.IgnoreFields.TRANSIENT);

        TestClassWithTransientFields obj1 = new TestClassWithTransientFields("1", "2");
        TestClassWithTransientFields obj2 = new TestClassWithTransientFields("1", "3");
        DiffNode diffNode = getDiffMapper().diff(obj1, obj2);

        assertThat(diffNode.getDiff().getOperation(), is(Diff.Operation.NO_OP));
        assertThat(diffNode.getFieldDiffs(), is(nullValue()));
    }

    @Override
    @Test
    public void isAbleToUseHashCodeForFastEqualityCheck() {
        getDiffMapper().enable(Feature.EqualityCheck.USE_HASHCODE);

        TestClassWithBrokenEqualityCheck obj1 = new TestClassWithBrokenEqualityCheck(1);
        TestClassWithBrokenEqualityCheck obj2 = new TestClassWithBrokenEqualityCheck(1);
        DiffNode diffNode = getDiffMapper().diff(obj1, obj2);

        assertThat(diffNode.isEmpty(), is(true));
    }

    @Override
    @Test
    public void customGlobalMergingHandler() {
        ObjectDiffMapper diffMapper = getDiffMapper();
        Date src = new Date();
        Date target = new Date(System.currentTimeMillis() + 10000L);
        diffMapper.registerMergingHandler(Date.class, new DateMergingHandler());
        Date result = diffMapper.applyDiff(src, new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE, src.getTime(), target.getTime())));
        assertThat(result != src, is(true));
        assertThat(result != target, is(true));
        assertThat(result.getTime(), is(target.getTime()));
    }

    @Override
    @Test
    public void customClassMergingHandler() {
        ObjectDiffMapper diffMapper = getDiffMapper();
        TestClassWithFieldTypeHandler src = new TestClassWithFieldTypeHandler(new Date());
        TestClassWithFieldTypeHandler target = new TestClassWithFieldTypeHandler(new Date(System.currentTimeMillis() + 10000L));

        DiffNode diffNode = new DiffNode(new Diff(),
                Collections.singletonMap("date", new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE,
                        src.date.getTime(), target.date.getTime()))));

        TestClassWithFieldTypeHandler result = diffMapper.applyDiff(src, diffNode);
        assertThat(result.date.getTime(), is(target.date.getTime()));
    }

    //endregion

}
