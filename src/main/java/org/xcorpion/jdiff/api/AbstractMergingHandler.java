package org.xcorpion.jdiff.api;

/**
 * This is to get around Java's runtime type erasure issue
 * such that code can directly get the expected field type information
 * and shortcut like {@link ObjectDiffMapper#registerMergingHandler(AbstractMergingHandler)}
 * is then possible.
 *
 * @param <T> type of the field to be merged
 */
public abstract class AbstractMergingHandler<T> implements MergingHandler<T> {
}
