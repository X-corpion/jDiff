package org.xcorpion.jdiff.testsuite;

import org.junit.Test;

public interface ObjectTestCases {

    @Test
    void diffObjectAgainstNull();

    @Test
    void diffTwoSimpleObjects();

    @Test
    void diffTwoObjectsWithNestedArrays();

    @Test
    void returnsNoDiffIfObjectsAreEqual();

    @Test
    void applyObjectDiff();

    @Test
    void applyObjectDiffOntoNewObject();
}
