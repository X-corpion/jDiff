package org.xcorpion.jdiff.handler;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffContext;
import org.xcorpion.jdiff.api.DiffNode;
import org.xcorpion.jdiff.api.DiffingHandler;

public class DateDiffingHandler implements DiffingHandler<Date> {

    @Nonnull
    @Override
    public DiffNode diff(@Nullable Date src, @Nullable Date target, @Nonnull DiffContext diffContext) {
        if (src == target) {
            return DiffNode.empty();
        }
        if (src == null || target == null) {
            return new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE, getMillis(src), getMillis(target)));
        }
        if (src.equals(target)) {
            return DiffNode.empty();
        }
        return new DiffNode(new Diff(Diff.Operation.UPDATE_VALUE, getMillis(src), getMillis(target)));
    }

    private static Long getMillis(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

}
