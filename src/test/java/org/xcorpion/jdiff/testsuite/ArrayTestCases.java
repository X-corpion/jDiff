package org.xcorpion.jdiff.testsuite;

import org.junit.Test;
import org.xcorpion.jdiff.exception.MergingException;

public interface ArrayTestCases {

    @Test
    void diffTwoBooleanPrimitiveArrays();

    @Test
    void applyDiffFromNullToPrimitive();

    @Test
    void applyPrimitiveDiffFromOneValueToAnother();

    @Test(expected = MergingException.class)
    void applyPrimitiveDiffToAWrongSrcShouldThrowException();

    @Test
    void applyPrimitiveDiffToAnUnequalButCompatibleSourceShouldNotThrowException();

    @Test
    void applyArrayDiffWithElementsRemoved();

    @Test
    void applyArrayDiffWithElementsAdded();

    @Test
    void applyArrayDiffWithValueUpdateOnly();
}
