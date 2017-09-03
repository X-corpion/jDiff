package org.xcorpion.jdiff.testsuite;

import org.junit.Test;

public interface ListTestCases {

    @Test
    void diffTwoStringLists();

    @Test
    void diffTwoStringLinkedLists();

    @Test
    void diffTwoListsWithNullValues();

    @Test
    void diffASmallerListWithALargerOne();

    @Test
    void diffALargerListWithASmallerOne();
}
