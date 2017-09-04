package org.xcorpion.jdiff.api;

import javax.annotation.Nullable;

public interface EqualityChecker<T> {

    boolean isEqualTo(@Nullable T src, @Nullable T target);

}
