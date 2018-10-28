package org.xcorpion.jdiff.handler;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.xcorpion.jdiff.api.AbstractMergingHandler;
import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffNode;
import org.xcorpion.jdiff.api.MergingContext;
import org.xcorpion.jdiff.exception.MergingException;

public class DateMergingHandler extends AbstractMergingHandler<Date> {

    @Nullable
    @Override
    public Date merge(@Nullable Date src, @Nonnull DiffNode diffNode,
            @Nonnull MergingContext mergingContext) throws MergingException {
        Diff diff = diffNode.getDiff();
        if (diff == null) {
            return src;
        }
        switch (diff.getOperation()) {
            case NO_OP:
                return src;
            case UPDATE_VALUE:
                validateSourceValue(getMillis(src), diffNode, mergingContext);
                Long target = (Long) diff.getTargetValue();
                if (target == null) {
                    return null;
                }
                return new Date(target);
        }
        return src;
    }

    private static Long getMillis(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

}
