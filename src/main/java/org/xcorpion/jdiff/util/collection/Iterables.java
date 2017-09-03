package org.xcorpion.jdiff.util.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Iterables {

    private static final Iterable<Object> EMPTY_ITERABLE = () -> new Iterator<Object>() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> Iterable<T> empty() {
        return (Iterable<T>) EMPTY_ITERABLE;
    }

}
