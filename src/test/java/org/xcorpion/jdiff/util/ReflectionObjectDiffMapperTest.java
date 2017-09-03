package org.xcorpion.jdiff.util;

import org.junit.Before;
import org.xcorpion.jdiff.api.*;
import org.xcorpion.jdiff.testsuite.ObjectDiffMapperTestSuite;

@SuppressWarnings("unchecked")
public class ReflectionObjectDiffMapperTest extends ObjectDiffMapperTestSuite {

    private ReflectionObjectDiffMapper diffMapper;

    @Override
    protected ObjectDiffMapper getDiffMapper() {
        return diffMapper;
    }

    @Before
    public void setUp() {
        diffMapper = new ReflectionObjectDiffMapper();
    }

}
