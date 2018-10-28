package org.xcorpion.jdiff.api;

/**
 * This is to get around Java's runtime type erasure issue
 * such that code can directly get the expected field type information
 * and shortcut like {@link ObjectDiffMapper#registerDiffingHandler(AbstractDiffingHandler)}
 * is then possible.
 *
 * @param <T> type of the field to be diff'ed
 */
public abstract class AbstractDiffingHandler<T> implements DiffingHandler<T> {
}
