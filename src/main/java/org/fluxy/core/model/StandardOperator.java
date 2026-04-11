package org.fluxy.core.model;

public enum StandardOperator implements Operator {

    EQ {
        @Override
        public boolean matches(Object expected, Object actual) {
            if (expected == null) return actual == null;
            return expected.equals(actual);
        }
    },

    NEQ {
        @Override
        public boolean matches(Object expected, Object actual) {
            return !EQ.matches(expected, actual);
        }
    },

    GT {
        @Override
        @SuppressWarnings("unchecked")
        public boolean matches(Object expected, Object actual) {
            if (expected instanceof Comparable && actual instanceof Comparable) {
                return ((Comparable<Object>) actual).compareTo(expected) > 0;
            }
            return false;
        }
    },

    LT {
        @Override
        @SuppressWarnings("unchecked")
        public boolean matches(Object expected, Object actual) {
            if (expected instanceof Comparable && actual instanceof Comparable) {
                return ((Comparable<Object>) actual).compareTo(expected) < 0;
            }
            return false;
        }
    },

    CONTAINS {
        @Override
        public boolean matches(Object expected, Object actual) {
            if (actual instanceof String actualStr && expected instanceof String expectedStr) {
                return actualStr.contains(expectedStr);
            }
            return false;
        }
    }
}

