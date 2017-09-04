package org.xcorpion.jdiff.api;

public interface Feature {

    interface Merging extends Feature {

    }

    interface Diffing extends Feature {

    }

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

    enum DiffingHandler implements Diffing {
        IGNORE_FIELD_TYPE_HANDLER,
        IGNORE_CLASS_TYPE_HANDLER,
        IGNORE_GLOBAL_TYPE_HANDLER
        ;

        @Override
        public boolean allowMultiple() {
            return true;
        }
    }

    enum MergingHandler implements Merging {
        IGNORE_FIELD_TYPE_HANDLER,
        IGNORE_CLASS_TYPE_HANDLER,
        IGNORE_GLOBAL_TYPE_HANDLER
        ;

        @Override
        public boolean allowMultiple() {
            return true;
        }
    }

    enum MergingValidationCheck implements Merging {
        VALIDATE_SOURCE_VALUE,
        VALIDATE_OBJECT_FIELD_EXISTENCE
        ;

        @Override
        public boolean allowMultiple() {
            return true;
        }
    }

    enum MergingStrategy implements Merging {

        SHALLOW_CLONE_SOURCE_ROOT,
        DEEP_CLONE_SOURCE,
        SHALLOW_CLONE_SOURCE_COLLECTIONS,

    }

}
