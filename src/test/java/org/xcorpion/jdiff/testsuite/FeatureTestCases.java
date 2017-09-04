package org.xcorpion.jdiff.testsuite;

import org.junit.Test;

public interface FeatureTestCases {

    @Test
    void canIgnoreTransientFields();

    @Test
    void isAbleToUseHashCodeForFastEqualityCheck();

    @Test
    void customGlobalDiffingHandler();

    @Test
    void customFieldDiffingHandler();

    @Test
    void customGlobalMergingHandler();

    @Test
    void customFieldMergingHandler();

}
