package org.fluxy.core.service;

import org.fluxy.core.model.*;

import java.util.Comparator;
import java.util.List;


public class StepExecutionService {

    private final TaskExecutorService taskExecutor;
    private final FluxyEventsBus fluxyEventsBus;

    public StepExecutionService(TaskExecutorService taskExecutor, FluxyEventsBus fluxyEventsBus) {
        this.taskExecutor = taskExecutor;
        this.fluxyEventsBus = fluxyEventsBus;
    }

    /**
     * Procesa la siguiente tarea pendiente o en ejecución del step dado.
     *
     * @param step     el step a procesar
     * @param context  el contexto de ejecución (datos de negocio)
     * @param metaInf  la traza de ejecución donde encontrar el step
     */
    public void processStep(FluxyStep step, ExecutionContext context, ExecutionMetaInf metaInf) {
        StepTask task = getTaskToExecute(metaInf, step);
        task.setStatus(TaskStatus.RUNNING);
        TaskResult result = taskExecutor.executeTask(task.getTask(), context);
        task.setResult(result);
        task.setStatus(TaskStatus.FINISHED);
    }

    private StepTask getTaskToExecute(ExecutionMetaInf metaInf, FluxyStep step) {
        List<StepTask> tasks = findCurrentStep(metaInf, step)
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

    private FluxyStep findCurrentStep(ExecutionMetaInf metaInf, FluxyStep step) {
        List<FlowStep> flowSteps = metaInf.getExecution()
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
