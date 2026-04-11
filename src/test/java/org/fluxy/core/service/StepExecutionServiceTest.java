package org.fluxy.core.service;

import org.fluxy.core.model.*;
import org.fluxy.core.support.InMemoryFluxyEventsBus;
import org.fluxy.core.support.SimpleTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StepExecutionServiceTest {

    private InMemoryFluxyEventsBus eventsBus;
    private TaskExecutorService taskExecutorService;
    private StepExecutionService stepExecutionService;

    @BeforeEach
    void setUp() {
        eventsBus = new InMemoryFluxyEventsBus();
        taskExecutorService = new TaskExecutorService(eventsBus);
        stepExecutionService = new StepExecutionService(taskExecutorService);
    }

    private ExecutionContext createContextWithStep(FluxyStep step) {
        ExecutionContext context = new ExecutionContext("test", "1.0");
        FlowStep flowStep = new FlowStep(1, step, StepStatus.RUNNING);
        ExecutionMetaInf metaInf = new ExecutionMetaInf();
        Map<FlowStep, List<StepTask>> execution = new LinkedHashMap<>();
        execution.put(flowStep, new ArrayList<>(step.getTasks()));
        metaInf.setExecution(execution);
        context.setExecutionMetaInf(metaInf);
        return context;
    }

    @Test
    void testProcessStepPicksPendingTask() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        StepTask stepTask = new StepTask(1, task);
        // status ya es PENDING por defecto

        FluxyStep step = new FluxyStep();
        step.setName("step1");
        step.setTasks(List.of(stepTask));

        ExecutionContext context = createContextWithStep(step);

        stepExecutionService.processStep(step, context);

        assertEquals(TaskStatus.FINISHED, stepTask.getStatus());
        assertEquals(TaskResult.SUCCESS, stepTask.getResult());
        assertEquals(1, task.getExecutionCount());
    }

    @Test
    void testProcessStepPicksRunningTaskOverPending() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("task2", TaskResult.SUCCESS);
        StepTask stepTask1 = new StepTask(1, task1);
        StepTask stepTask2 = new StepTask(2, task2);
        stepTask1.setStatus(TaskStatus.RUNNING);
        // stepTask2 es PENDING

        FluxyStep step = new FluxyStep();
        step.setName("step1");
        step.setTasks(List.of(stepTask1, stepTask2));

        ExecutionContext context = createContextWithStep(step);

        stepExecutionService.processStep(step, context);

        // Debe haber ejecutado task1 (RUNNING) y no task2 (PENDING)
        assertEquals(TaskStatus.FINISHED, stepTask1.getStatus());
        assertEquals(TaskResult.SUCCESS, stepTask1.getResult());
        assertEquals(1, task1.getExecutionCount());
        assertEquals(TaskStatus.PENDING, stepTask2.getStatus());
        assertEquals(0, task2.getExecutionCount());
    }

    @Test
    void testProcessStepUpdatesStatusTransition() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        StepTask stepTask = new StepTask(1, task);

        FluxyStep step = new FluxyStep();
        step.setName("step1");
        step.setTasks(List.of(stepTask));

        ExecutionContext context = createContextWithStep(step);

        // Antes: PENDING
        assertEquals(TaskStatus.PENDING, stepTask.getStatus());

        stepExecutionService.processStep(step, context);

        // Después: FINISHED con resultado
        assertEquals(TaskStatus.FINISHED, stepTask.getStatus());
        assertEquals(TaskResult.SUCCESS, stepTask.getResult());
    }

    @Test
    void testProcessStepMultipleTasksInOrder() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("task2", TaskResult.SUCCESS);
        StepTask stepTask1 = new StepTask(1, task1);
        StepTask stepTask2 = new StepTask(2, task2);

        FluxyStep step = new FluxyStep();
        step.setName("step1");
        step.setTasks(new ArrayList<>(List.of(stepTask1, stepTask2)));

        ExecutionContext context = createContextWithStep(step);

        // Primera ejecución: debe procesar task1 (primer PENDING en orden)
        stepExecutionService.processStep(step, context);
        assertEquals(TaskStatus.FINISHED, stepTask1.getStatus());
        assertEquals(TaskStatus.PENDING, stepTask2.getStatus());
        assertEquals(1, task1.getExecutionCount());
        assertEquals(0, task2.getExecutionCount());

        // Segunda ejecución: debe procesar task2 (siguiente PENDING)
        stepExecutionService.processStep(step, context);
        assertEquals(TaskStatus.FINISHED, stepTask2.getStatus());
        assertEquals(1, task2.getExecutionCount());
    }

    @Test
    void testProcessStepNoTaskAvailableThrows() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        StepTask stepTask = new StepTask(1, task);
        stepTask.setStatus(TaskStatus.FINISHED);

        FluxyStep step = new FluxyStep();
        step.setName("step1");
        step.setTasks(List.of(stepTask));

        ExecutionContext context = createContextWithStep(step);

        assertThrows(IllegalStateException.class,
                () -> stepExecutionService.processStep(step, context));
    }

    @Test
    void testProcessStepWithFailureResult() {
        SimpleTask task = new SimpleTask("task1", TaskResult.FAILURE);
        StepTask stepTask = new StepTask(1, task);

        FluxyStep step = new FluxyStep();
        step.setName("step1");
        step.setTasks(List.of(stepTask));

        ExecutionContext context = createContextWithStep(step);

        stepExecutionService.processStep(step, context);

        assertEquals(TaskStatus.FINISHED, stepTask.getStatus());
        assertEquals(TaskResult.FAILURE, stepTask.getResult());
    }
}

