package org.fluxy.core.support;

import org.fluxy.core.model.ExecutionContext;
import org.fluxy.core.model.FluxyTask;
import org.fluxy.core.model.TaskResult;

/**
 * Task que lanza una excepción al ejecutarse. Útil para tests de manejo de errores.
 */
public class FailingTask extends FluxyTask {

    private final RuntimeException exception;

    public FailingTask(String name, RuntimeException exception) {
        this.name = name;
        this.exception = exception;
    }

    @Override
    public TaskResult execute(ExecutionContext executionContext) {
        throw exception;
    }
}

