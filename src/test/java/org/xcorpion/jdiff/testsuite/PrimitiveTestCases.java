package org.xcorpion.jdiff.testsuite;

@SuppressWarnings("unused")
public interface PrimitiveTestCases {

    void diffTwoPrimitiveIntegers();

    void diffTwoBoxedIntegers();

    void diffTwoPrimitiveBooleans();

    void diffTwoBoxedBooleans();

    void diffTwoStrings();

    void diffTwoNulls();

    void diffIntegerAgainstNull();

    void diffNullAgainstInteger();

    void diffBooleanAgainstNull();

}
