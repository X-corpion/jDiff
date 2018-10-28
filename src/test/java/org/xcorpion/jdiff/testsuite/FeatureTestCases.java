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
    void customFieldDiffingHandlerUsingAnnotation();

    @Test
    void customFieldDiffingHandlerUsingRegisterCall();

    @Test
    void customDiffingHandlerCanHandleGenerics();

    @Test
    void customGlobalMergingHandler();

    @Test
    void customMergingHandlerCanHandleGenerics();

    @Test
    void customFieldMergingHandler();

}
