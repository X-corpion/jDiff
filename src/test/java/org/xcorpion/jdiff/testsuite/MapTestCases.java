package org.xcorpion.jdiff.testsuite;

import org.junit.Test;

public interface MapTestCases {

    @Test
    void diffTwoMapsWithBaseType();

    @Test
    void diffTwoMapsWithNullKey();

    @Test
    void diffALargerMapWithASmallerOne();

    @Test
    void diffASmallerMapWithALargerOne();

    @Test
    void diffMapsWithNonPrimitiveKeys();

    @Test
    void applyMapDiffWithSimpleKey();

    @Test
    void applyMapDiffWithObjectKey();
}
