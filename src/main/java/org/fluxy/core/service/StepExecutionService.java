package org.fluxy.core.service;

import org.fluxy.core.model.*;

import java.util.Comparator;
import java.util.List;


public class StepExecutionService {

    private final TaskExecutorService taskExecutor;

    public StepExecutionService(TaskExecutorService taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public void processStep(FluxyStep step, ExecutionContext executionContext) {
        StepTask task = getTaskToExecute(executionContext, step);
        task.setStatus(TaskStatus.RUNNING);
        TaskResult result = taskExecutor.executeTask(task.getTask(), executionContext);
        task.setResult(result);
        task.setStatus(TaskStatus.FINISHED);
    }

    private StepTask getTaskToExecute(ExecutionContext executionContext, FluxyStep step) {
        List<StepTask> tasks = findCurrentStep(executionContext, step)
                .getTasks()
                .stream()
                .sorted(Comparator.comparing(StepTask::getOrder))
                .toList();

        return tasks.stream()
                .filter(stepTask -> stepTask.getStatus() == TaskStatus.RUNNING)
                .findFirst()
                .orElseGet(() -> tasks.stream()
                        .filter(stepTask -> stepTask.getStatus() == TaskStatus.PENDING)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No pending or running task found for step: " + step.getName())));
    }

    private FluxyStep findCurrentStep(ExecutionContext executionContext, FluxyStep step) {
        List<FlowStep> flowSteps = executionContext.getExecutionMetaInf().getExecution()
            .keySet()
            .stream()
            .filter(flowStep -> flowStep.getStep().equals(step))
            .toList();
        if(flowSteps.size() != 1) {
            throw new IllegalStateException("Expected exactly one FlowStep matching step: " + step.getName() + ", found: " + flowSteps.size());
        }
        return flowSteps.getFirst().getStep();
    }


}
