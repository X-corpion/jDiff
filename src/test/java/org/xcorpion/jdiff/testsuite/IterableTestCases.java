package org.xcorpion.jdiff.testsuite;

import org.junit.Test;

public interface IterableTestCases {

    @Test
    void iterableMergingShouldThrowExceptionByDefault();

    @Test
    void iterableMergingCanBeDoneWithCustomMerger();

}
