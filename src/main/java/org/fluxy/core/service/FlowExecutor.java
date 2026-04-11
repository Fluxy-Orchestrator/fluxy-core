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

    public void initializeExecution(FluxyFlow flow, ExecutionContext executionContext) {
        ExecutionMetaInf metaInf = new ExecutionMetaInf();
        Map<FlowStep, List<StepTask>> execution = new LinkedHashMap<>();
        for (FlowStep flowStep : flow.getSteps()) {
            flowStep.setStepStatus(StepStatus.PENDING);
            List<StepTask> tasks = flowStep.getStep().getTasks();
            if (tasks != null) {
                tasks.forEach(task -> {
                    task.setStatus(TaskStatus.PENDING);
                    task.setResult(null);
                });
            }
            execution.put(flowStep, tasks != null ? new ArrayList<>(tasks) : new ArrayList<>());
        }
        metaInf.setExecution(execution);
        executionContext.setExecutionMetaInf(metaInf);
    }

    public void processFlow(FluxyFlow flow, ExecutionContext executionContext) {
        FlowStep currentFlowStep;
        if (isStepRunning(executionContext)) {
            currentFlowStep = findRunningFlowStep(executionContext);
        } else {
            currentFlowStep = resolveNextStep(flow, executionContext);
            currentFlowStep.setStepStatus(StepStatus.RUNNING);
        }

        stepExecutionService.processStep(currentFlowStep.getStep(), executionContext);

        if (isStepComplete(currentFlowStep)) {
            currentFlowStep.setStepStatus(StepStatus.FINISHED);
        }
    }

    private boolean isStepRunning(ExecutionContext executionContext) {
        return executionContext.getExecutionMetaInf().getExecution().keySet()
                .stream()
                .anyMatch(flowStep -> flowStep.getStepStatus() == StepStatus.RUNNING);
    }

    private FlowStep findRunningFlowStep(ExecutionContext executionContext) {
        return executionContext.getExecutionMetaInf().getExecution().keySet()
                .stream()
                .filter(flowStep -> flowStep.getStepStatus() == StepStatus.RUNNING)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No running step found"));
    }

    private FlowStep resolveNextStep(FluxyFlow flow, ExecutionContext executionContext) {
        Optional<FlowStep> lastFinished = findLastFinishedStep(executionContext);

        if (lastFinished.isPresent() && flow.getConnections() != null && !flow.getConnections().isEmpty()) {
            Optional<FlowStep> target = flow.getConnections().stream()
                    .filter(conn -> conn.getFromStep().equals(lastFinished.get()))
                    .filter(conn -> evaluateConditions(conn, executionContext))
                    .map(Connection::getToStep)
                    .findFirst();
            if (target.isPresent()) {
                return target.get();
            }
        }

        return findNextPendingFlowStep(executionContext);
    }

    private boolean evaluateConditions(Connection connection, ExecutionContext executionContext) {
        if (connection.getConditions() == null || connection.getConditions().isEmpty()) {
            return true;
        }
        return connection.getConditions().stream().allMatch(c -> c.evaluate(executionContext));
    }

    private Optional<FlowStep> findLastFinishedStep(ExecutionContext executionContext) {
        return executionContext.getExecutionMetaInf().getExecution().keySet()
                .stream()
                .filter(fs -> fs.getStepStatus() == StepStatus.FINISHED)
                .max(Comparator.comparing(FlowStep::getOrder));
    }

    private FlowStep findNextPendingFlowStep(ExecutionContext executionContext) {
        return executionContext.getExecutionMetaInf().getExecution().keySet()
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
