package org.xcorpion.jdiff.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.xcorpion.jdiff.api.Diff;
import org.xcorpion.jdiff.api.DiffNode;
import org.xcorpion.jdiff.api.MergingContext;
import org.xcorpion.jdiff.api.MergingHandler;
import org.xcorpion.jdiff.exception.MergingException;

public class TestIterableMergingHandler implements MergingHandler<Iterable<Object>> {

    @Nullable
    @Override
    public Iterable<Object> merge(@Nullable Iterable<Object> src, @Nonnull DiffNode diffNode,
            @Nonnull MergingContext mergingContext) throws MergingException {
        List<Object> list = new ArrayList<>();
        if (src == null) {
            return list;
        }
        src.forEach(list::add);
        if (diffNode.getFieldDiffs() == null) {
            return list;
        }
        List<Integer> indicesToRemove = new ArrayList<>();
        diffNode.getFieldDiffs().forEach((key, value) -> {
            Diff diff = value.getDiff();
            int index = (int) key;
            if (diff != null) {
                switch (diff.getOperation()) {
                    case UPDATE_VALUE:
                        list.set(index, diff.getTargetValue());
                        break;
                    case ADD_VALUE:
                        list.add(diff.getTargetValue());
                        break;
                    case REMOVE_VALUE:
                        indicesToRemove.add(index);
                        break;
                    default:
                        throw new MergingException("Unexpected operation: " + diff.getOperation());
                }
            }
        });
        indicesToRemove.sort(Comparator.reverseOrder());
        indicesToRemove.forEach(i -> list.remove(i.intValue()));
        return list;
    }

}
