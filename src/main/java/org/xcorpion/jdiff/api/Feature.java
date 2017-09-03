package org.xcorpion.jdiff.api;

public interface Feature {

    default boolean allowMultiple() {
        return false;
    }

    enum EqualityCheck implements Feature {
        USE_HASHCODE,
        USE_EQUALS
    }

    enum IgnoreFields implements Feature {
        TRANSIENT,
        INACCESSIBLE
        ;

        @Override
        public boolean allowMultiple() {
            return true;
        }
    }

}
