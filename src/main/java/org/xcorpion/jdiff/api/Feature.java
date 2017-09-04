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

    enum TypeHandler implements Feature {
        IGNORE_FIELD_TYPE_HANDLER_FOR_MERGING,
        IGNORE_CLASS_TYPE_HANDLER_FOR_MERGING,
        IGNORE_GLOBAL_TYPE_HANDLER_FOR_MERGING
        ;

        @Override
        public boolean allowMultiple() {
            return true;
        }
    }

    enum MergingValidationCheck implements Feature {
        VALIDATE_SOURCE_VALUE,
        VALIDATE_OBJECT_FIELD_EXISTENCE
        ;

        @Override
        public boolean allowMultiple() {
            return true;
        }
    }

    enum MergingStrategy implements Feature {

        SHALLOW_CLONE_SOURCE_ROOT,
        DEEP_CLONE_SOURCE,
        SHALLOW_CLONE_SOURCE_COLLECTIONS,

    }

}
