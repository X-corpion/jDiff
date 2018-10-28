package org.xcorpion.jdiff.testsuite.handlers;

import org.apache.commons.lang3.StringUtils;
import org.xcorpion.jdiff.api.AbstractMergingHandler;
import org.xcorpion.jdiff.api.DiffNode;
import org.xcorpion.jdiff.api.MergingContext;
import org.xcorpion.jdiff.exception.MergingException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringToStringListMergingHandler extends AbstractMergingHandler<List<String>> {
    @Nullable
    @Override
    public List<String> merge(@Nullable List<String> src, @Nonnull DiffNode diffNode,
            @Nonnull MergingContext mergingContext) throws MergingException {
        String targetValue = (String) diffNode.getDiff().getTargetValue();
        src = new ArrayList<>();
        src.addAll(Arrays.asList(StringUtils.split(targetValue, '|')));
        return src;
    }
}
