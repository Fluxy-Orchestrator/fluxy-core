package org.fluxy.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Condition {

    private Operator operator;
    private Object value;
    private String variablePath;

    public boolean evaluate(ExecutionContext executionContext) {
        Object variableValue = executionContext.getVariableByPath(variablePath);
        return operator.matches(value, variableValue);
    }
}
