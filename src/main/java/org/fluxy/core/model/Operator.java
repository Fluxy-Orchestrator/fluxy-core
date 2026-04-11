package org.fluxy.core.model;

public interface Operator {

    boolean matches(Object value, Object variableValue);
}
