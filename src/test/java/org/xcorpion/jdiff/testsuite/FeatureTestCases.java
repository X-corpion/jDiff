package org.xcorpion.jdiff.testsuite;

import org.junit.Test;

public interface FeatureTestCases {

    @Test
    void canIgnoreTransientFields();

    @Test
    void isAbleToUseHashCodeForFastEqualityCheck();

    @Test
    void customGlobalMergingHandler();

    @Test
    void customClassMergingHandler();

}
