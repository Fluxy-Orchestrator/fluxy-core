package org.fluxy.core.support;

import org.fluxy.core.model.ExecutionContext;
import org.fluxy.core.model.FluxyTask;
import org.fluxy.core.model.TaskResult;

/**
 * Implementación concreta simple de FluxyTask para uso en tests.
 * Retorna un TaskResult predefinido y opcionalmente registra una variable en el contexto.
 */
public class SimpleTask extends FluxyTask {

    private final TaskResult expectedResult;
    private final String variableToSet;
    private final String variableValue;
    private int executionCount = 0;

    public SimpleTask(String name, TaskResult expectedResult) {
        this.name = name;
        this.expectedResult = expectedResult;
        this.variableToSet = null;
        this.variableValue = null;
    }

    public SimpleTask(String name, TaskResult expectedResult, String variableToSet, String variableValue) {
        this.name = name;
        this.expectedResult = expectedResult;
        this.variableToSet = variableToSet;
        this.variableValue = variableValue;
    }

    @Override
    public TaskResult execute(ExecutionContext executionContext) {
        executionCount++;
        if (variableToSet != null) {
            executionContext.addParameter(variableToSet, variableValue);
        }
        return expectedResult;
    }

    public int getExecutionCount() {
        return executionCount;
    }
}

