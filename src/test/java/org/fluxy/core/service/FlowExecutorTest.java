package org.fluxy.core.service;

import org.fluxy.core.model.*;
import org.fluxy.core.support.InMemoryFluxyEventsBus;
import org.fluxy.core.support.SimpleTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FlowExecutorTest {

    private InMemoryFluxyEventsBus eventsBus;
    private TaskExecutorService taskExecutorService;
    private StepExecutionService stepExecutionService;
    private FlowExecutor flowExecutor;

    @BeforeEach
    void setUp() {
        eventsBus = new InMemoryFluxyEventsBus();
        taskExecutorService = new TaskExecutorService(eventsBus);
        stepExecutionService = new StepExecutionService(taskExecutorService, eventsBus);
        flowExecutor = new FlowExecutor(eventsBus, stepExecutionService);
    }

    // ===== Helper methods =====

    private FluxyStep createStep(String name, FluxyTask... tasks) {
        FluxyStep step = new FluxyStep();
        step.setName(name);
        List<StepTask> stepTasks = new ArrayList<>();
        for (int i = 0; i < tasks.length; i++) {
            stepTasks.add(new StepTask(i + 1, tasks[i]));
        }
        step.setTasks(stepTasks);
        return step;
    }

    private FluxyFlow createLinearFlow(String name, FlowStep... flowSteps) {
        FluxyFlow flow = new FluxyFlow();
        flow.setName(name);
        flow.setSteps(Arrays.asList(flowSteps));
        flow.setConnections(new ArrayList<>());
        return flow;
    }

    private ExecutionContext createContext(String type) {
        ExecutionContext ctx = new ExecutionContext(type, "1.0");
        ctx.addReference("testRef", "testValue");
        return ctx;
    }

    // ===== initializeExecution tests =====

    @Test
    void testInitializeExecution() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("task2", TaskResult.SUCCESS);

        FluxyStep step1 = createStep("step1", task1);
        FluxyStep step2 = createStep("step2", task2);

        FlowStep flowStep1 = new FlowStep(1, step1, null);
        FlowStep flowStep2 = new FlowStep(2, step2, null);

        FluxyFlow flow = createLinearFlow("test", flowStep1, flowStep2);
        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);

        flowExecutor.initializeExecution(execution);

        assertNotNull(execution.getMetaInf());
        Map<FlowStep, List<StepTask>> executionMap = execution.getMetaInf().getExecution();
        assertEquals(2, executionMap.size());

        // Todos los FlowSteps deben estar en PENDING
        for (FlowStep fs : executionMap.keySet()) {
            assertEquals(StepStatus.PENDING, fs.getStepStatus());
        }

        // Todas las tasks deben estar en PENDING
        for (List<StepTask> tasks : executionMap.values()) {
            for (StepTask st : tasks) {
                assertEquals(TaskStatus.PENDING, st.getStatus());
            }
        }
    }

    // ===== processFlow - flujo lineal =====

    @Test
    void testProcessFlowLinearSingleStep() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        FluxyStep step = createStep("step1", task);
        FlowStep flowStep = new FlowStep(1, step, null);

        FluxyFlow flow = createLinearFlow("test", flowStep);
        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        flowExecutor.processFlow(execution);

        assertEquals(StepStatus.FINISHED, flowStep.getStepStatus());
        assertEquals(TaskStatus.FINISHED, step.getTasks().getFirst().getStatus());
        assertEquals(TaskResult.SUCCESS, step.getTasks().getFirst().getResult());
    }

    @Test
    void testProcessFlowLinearTwoSteps() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("task2", TaskResult.SUCCESS);
        FluxyStep step1 = createStep("step1", task1);
        FluxyStep step2 = createStep("step2", task2);

        FlowStep flowStep1 = new FlowStep(1, step1, null);
        FlowStep flowStep2 = new FlowStep(2, step2, null);

        FluxyFlow flow = createLinearFlow("test", flowStep1, flowStep2);
        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Primera llamada: ejecuta step1
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStep1.getStepStatus());
        assertEquals(StepStatus.PENDING, flowStep2.getStepStatus());

        // Segunda llamada: ejecuta step2
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStep2.getStepStatus());
    }

    // ===== processFlow - paso en curso (RUNNING) =====

    @Test
    void testProcessFlowContinuesRunningStep() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("task2", TaskResult.SUCCESS);
        FluxyStep step = createStep("step1", task1, task2);

        FlowStep flowStep = new FlowStep(1, step, null);
        FluxyFlow flow = createLinearFlow("test", flowStep);
        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Primera llamada: ejecuta task1, step queda RUNNING (task2 aún PENDING)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.RUNNING, flowStep.getStepStatus());
        assertEquals(TaskStatus.FINISHED, step.getTasks().get(0).getStatus());
        assertEquals(TaskStatus.PENDING, step.getTasks().get(1).getStatus());

        // Segunda llamada: continúa con step en RUNNING, ejecuta task2
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStep.getStepStatus());
        assertEquals(TaskStatus.FINISHED, step.getTasks().get(1).getStatus());
    }

    // ===== processFlow - bifurcación por conexiones =====

    @Test
    void testProcessFlowBranchingConditionMet() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS, "route", "branchA");
        SimpleTask taskA = new SimpleTask("taskA", TaskResult.SUCCESS);
        SimpleTask taskB = new SimpleTask("taskB", TaskResult.SUCCESS);

        FluxyStep step1 = createStep("step1", task1);
        FluxyStep stepA = createStep("stepA", taskA);
        FluxyStep stepB = createStep("stepB", taskB);

        FlowStep flowStep1 = new FlowStep(1, step1, null);
        FlowStep flowStepA = new FlowStep(2, stepA, null);
        FlowStep flowStepB = new FlowStep(3, stepB, null);

        // Conexión: si "route" == "branchA", ir a stepA
        Connection connToA = new Connection(flowStep1, flowStepA,
                List.of(new Condition(StandardOperator.EQ, "branchA", "route")));

        FluxyFlow flow = new FluxyFlow();
        flow.setName("test");
        flow.setSteps(List.of(flowStep1, flowStepA, flowStepB));
        flow.setConnections(List.of(connToA));

        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Ejecuta step1 (agrega variable route=branchA al contexto)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStep1.getStepStatus());

        // Ejecuta: debe seguir la conexión a stepA (no stepB secuencial)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStepA.getStepStatus());
        assertEquals(StepStatus.PENDING, flowStepB.getStepStatus());
        assertEquals(1, taskA.getExecutionCount());
        assertEquals(0, taskB.getExecutionCount());
    }

    @Test
    void testProcessFlowBranchingConditionNotMet_fallsBackToSequential() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS, "route", "other");
        SimpleTask taskA = new SimpleTask("taskA", TaskResult.SUCCESS);
        SimpleTask taskB = new SimpleTask("taskB", TaskResult.SUCCESS);

        FluxyStep step1 = createStep("step1", task1);
        FluxyStep stepA = createStep("stepA", taskA);
        FluxyStep stepB = createStep("stepB", taskB);

        FlowStep flowStep1 = new FlowStep(1, step1, null);
        FlowStep flowStepA = new FlowStep(2, stepA, null);
        FlowStep flowStepB = new FlowStep(3, stepB, null);

        // Conexión: si "route" == "branchA", ir a stepB (orden 3) — pero condición no se cumple
        Connection connToB = new Connection(flowStep1, flowStepB,
                List.of(new Condition(StandardOperator.EQ, "branchA", "route")));

        FluxyFlow flow = new FluxyFlow();
        flow.setName("test");
        flow.setSteps(List.of(flowStep1, flowStepA, flowStepB));
        flow.setConnections(List.of(connToB));

        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Ejecuta step1 (agrega variable route=other)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStep1.getStepStatus());

        // Condición no se cumple → cae al siguiente PENDING en orden (stepA, orden 2)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStepA.getStepStatus());
        assertEquals(StepStatus.PENDING, flowStepB.getStepStatus());
    }

    @Test
    void testProcessFlowBranchingWithNoConditions_alwaysMatches() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask taskA = new SimpleTask("taskA", TaskResult.SUCCESS);
        SimpleTask taskB = new SimpleTask("taskB", TaskResult.SUCCESS);

        FluxyStep step1 = createStep("step1", task1);
        FluxyStep stepA = createStep("stepA", taskA);
        FluxyStep stepB = createStep("stepB", taskB);

        FlowStep flowStep1 = new FlowStep(1, step1, null);
        FlowStep flowStepA = new FlowStep(2, stepA, null);
        FlowStep flowStepB = new FlowStep(3, stepB, null);

        // Conexión sin condiciones → siempre se toma
        Connection unconditional = new Connection(flowStep1, flowStepB, null);

        FluxyFlow flow = new FluxyFlow();
        flow.setName("test");
        flow.setSteps(List.of(flowStep1, flowStepA, flowStepB));
        flow.setConnections(List.of(unconditional));

        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStep1.getStepStatus());

        // Salta a stepB directamente (salteando stepA)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStepB.getStepStatus());
        assertEquals(StepStatus.PENDING, flowStepA.getStepStatus());
    }

    // ===== processFlow - sin pasos pendientes =====

    @Test
    void testProcessFlowNoPendingStepThrows() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        FluxyStep step = createStep("step1", task);
        FlowStep flowStep = new FlowStep(1, step, null);

        FluxyFlow flow = createLinearFlow("test", flowStep);
        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Ejecuta el único step
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, flowStep.getStepStatus());

        // Ya no hay pasos pendientes
        assertThrows(IllegalStateException.class,
                () -> flowExecutor.processFlow(execution));
    }

    // ===== processFlow - verifica eventos publicados =====

    @Test
    void testProcessFlowPublishesEventsForEachTask() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("task2", TaskResult.SUCCESS);
        FluxyStep step1 = createStep("step1", task1);
        FluxyStep step2 = createStep("step2", task2);

        FlowStep flowStep1 = new FlowStep(1, step1, null);
        FlowStep flowStep2 = new FlowStep(2, step2, null);

        FluxyFlow flow = createLinearFlow("test", flowStep1, flowStep2);
        ExecutionContext context = createContext("test");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        flowExecutor.processFlow(execution);
        flowExecutor.processFlow(execution);

        assertEquals(2, eventsBus.getPublishedEvents().size());
    }
}

