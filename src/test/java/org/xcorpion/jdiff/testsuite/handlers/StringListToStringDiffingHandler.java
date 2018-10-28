package org.xcorpion.jdiff.testsuite.handlers;

import org.xcorpion.jdiff.api.AbstractDiffingHandler;
import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffNode;
import org.xcorpion.jdiff.api.DiffingContext;

import javax.annotation.Nonnull;
import java.util.List;

public class StringListToStringDiffingHandler extends AbstractDiffingHandler<List<String>> {
    @Nonnull
    @Override
    public DiffNode diff(@Nonnull List<String> src, @Nonnull List<String> target, @Nonnull DiffingContext diffingContext) {
        if (src.equals(target)) {
            return new DiffNode();
        }
        Diff diff = new Diff(Diff.Operation.UPDATE_VALUE, String.join("|", src), String.join("|", target));
        return new DiffNode(diff);
    }
}
