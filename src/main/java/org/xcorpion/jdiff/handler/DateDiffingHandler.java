package org.xcorpion.jdiff.handler;

import javax.annotation.Nonnull;
import java.util.Date;

import org.xcorpion.jdiff.api.AbstractDiffingHandler;
import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffingContext;
import org.xcorpion.jdiff.api.DiffNode;

public class DateDiffingHandler extends AbstractDiffingHandler<Date> {

    @Nonnull
    @Override
    public DiffNode diff(@Nonnull Date src, @Nonnull Date target, @Nonnull DiffingContext diffingContext) {
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
