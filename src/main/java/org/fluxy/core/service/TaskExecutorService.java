package org.fluxy.core.service;

import org.fluxy.core.exception.TaskExecutionException;
import org.fluxy.core.model.*;

public class TaskExecutorService {

    private final FluxyEventsBus eventsBus;

    public TaskExecutorService(FluxyEventsBus eventsBus) {
        this.eventsBus = eventsBus;
    }

    public TaskResult executeTask(FluxyTask task, ExecutionContext context) {
        try {
            TaskResult result = task.execute(context);
            eventsBus.publish(new FluxyEvent<>(task, result, context));
            return result;
        } catch (Exception unhandledException) {
            eventsBus.publish(new FluxyEvent<>(task, unhandledException, context));
            throw new TaskExecutionException("Error executing task: " + task.getName(), unhandledException);
        }
    }
}
