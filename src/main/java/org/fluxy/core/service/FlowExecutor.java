package org.fluxy.core.service;

import org.fluxy.core.model.*;

import java.util.*;

public class FlowExecutor {

    private final FluxyEventsBus eventsBus;
    private final StepExecutionService stepExecutionService;

    public FlowExecutor(final FluxyEventsBus eventsBus, final StepExecutionService stepExecutionService) {
        this.eventsBus = eventsBus;
        this.stepExecutionService = stepExecutionService;
    }

    /**
     * Inicializa la ejecución: prepara el {@link ExecutionMetaInf} dentro del
     * {@link FluxyExecution} a partir de la definición del flow.
     * Resetea todos los steps a PENDING y todas las tasks a PENDING/null.
     *
     * @param execution la ejecución a inicializar
     */
    public void initializeExecution(FluxyExecution execution) {
        FluxyFlow flow = execution.getFlow();
        ExecutionMetaInf metaInf = execution.getMetaInf();

        Map<FlowStep, List<StepTask>> executionMap = new LinkedHashMap<>();
        for (FlowStep flowStep : flow.getSteps()) {
            flowStep.setStepStatus(StepStatus.PENDING);
            List<StepTask> tasks = flowStep.getStep().getTasks();
            if (tasks != null) {
                tasks.forEach(task -> {
                    task.setStatus(TaskStatus.PENDING);
                    task.setResult(null);
                });
            }
            executionMap.put(flowStep, tasks != null ? new ArrayList<>(tasks) : new ArrayList<>());
        }
        metaInf.setExecution(executionMap);
    }

    /**
     * Procesa UN paso del flow: encuentra el step actualmente en RUNNING o
     * resuelve el siguiente PENDING, y ejecuta una task dentro de él.
     *
     * @param execution la ejecución en curso
     */
    public void processFlow(FluxyExecution execution) {
        FluxyFlow flow = execution.getFlow();
        ExecutionContext context = execution.getContext();
        ExecutionMetaInf metaInf = execution.getMetaInf();

        FlowStep currentFlowStep;
        if (isStepRunning(metaInf)) {
            currentFlowStep = findRunningFlowStep(metaInf);
        } else {
            currentFlowStep = resolveNextStep(flow, metaInf, context);
            currentFlowStep.setStepStatus(StepStatus.RUNNING);
        }

        stepExecutionService.processStep(currentFlowStep.getStep(), context, metaInf);

        if (isStepComplete(currentFlowStep)) {
            currentFlowStep.setStepStatus(StepStatus.FINISHED);
        }
    }

    private boolean isStepRunning(ExecutionMetaInf metaInf) {
        return metaInf.getExecution().keySet()
                .stream()
                .anyMatch(flowStep -> flowStep.getStepStatus() == StepStatus.RUNNING);
    }

    private FlowStep findRunningFlowStep(ExecutionMetaInf metaInf) {
        return metaInf.getExecution().keySet()
                .stream()
                .filter(flowStep -> flowStep.getStepStatus() == StepStatus.RUNNING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No running step found"));
    }

    private FlowStep resolveNextStep(FluxyFlow flow, ExecutionMetaInf metaInf, ExecutionContext context) {
        Optional<FlowStep> lastFinished = findLastFinishedStep(metaInf);

        if (lastFinished.isPresent() && flow.getConnections() != null && !flow.getConnections().isEmpty()) {
            Optional<FlowStep> target = flow.getConnections().stream()
                    .filter(conn -> conn.getFromStep().equals(lastFinished.get()))
                    .filter(conn -> evaluateConditions(conn, context))
                    .map(Connection::getToStep)
                    .findFirst();
            if (target.isPresent()) {
                return target.get();
            }
        }

        return findNextPendingFlowStep(metaInf);
    }

    private boolean evaluateConditions(Connection connection, ExecutionContext context) {
        if (connection.getConditions() == null || connection.getConditions().isEmpty()) {
            return true;
        }
        return connection.getConditions().stream().allMatch(c -> c.evaluate(context));
    }

    private Optional<FlowStep> findLastFinishedStep(ExecutionMetaInf metaInf) {
        return metaInf.getExecution().keySet()
                .stream()
                .filter(fs -> fs.getStepStatus() == StepStatus.FINISHED)
                .max(Comparator.comparing(FlowStep::getOrder));
    }

    private FlowStep findNextPendingFlowStep(ExecutionMetaInf metaInf) {
        return metaInf.getExecution().keySet()
                .stream()
                .filter(flowStep -> flowStep.getStepStatus() == StepStatus.PENDING)
                .sorted(Comparator.comparing(FlowStep::getOrder))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No pending step found in flow"));
    }

    private boolean isStepComplete(FlowStep flowStep) {
        return flowStep.getStep().getTasks().stream()
                .allMatch(task -> task.getStatus() == TaskStatus.FINISHED);
    }
}
