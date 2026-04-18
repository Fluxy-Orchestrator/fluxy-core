package org.fluxy.core;

import org.fluxy.core.model.*;
import org.fluxy.core.service.*;
import org.fluxy.core.support.InMemoryFluxyEventsBus;
import org.fluxy.core.support.SimpleTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración end-to-end que verifican la ejecución completa de flujos
 * con múltiples niveles (Task → StepTask → FluxyStep → FlowStep → FluxyFlow)
 * y el contexto de ejecución coordinando toda la traza a través de FluxyExecution.
 */
class FlowIntegrationTest {

    private InMemoryFluxyEventsBus eventsBus;
    private FlowExecutor flowExecutor;

    @BeforeEach
    void setUp() {
        eventsBus = new InMemoryFluxyEventsBus();
        TaskExecutorService taskExecutorService = new TaskExecutorService(eventsBus);
        StepExecutionService stepExecutionService = new StepExecutionService(taskExecutorService, eventsBus);
        flowExecutor = new FlowExecutor(eventsBus, stepExecutionService);
    }

    // ===== Helpers =====

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

    private ExecutionContext createContext(String type) {
        ExecutionContext ctx = new ExecutionContext(type, "1.0");
        ctx.addReference("testRef", "testValue");
        return ctx;
    }

    // =========================================================================
    // Test 1: Flujo lineal completo — 3 steps secuenciales, cada uno con 1 task
    // =========================================================================

    @Test
    void testLinearFlowExecution_threeSteps() {
        // Configurar tasks
        SimpleTask validateTask = new SimpleTask("validate", TaskResult.SUCCESS, "validated", "true");
        SimpleTask processTask = new SimpleTask("process", TaskResult.SUCCESS, "processed", "true");
        SimpleTask notifyTask = new SimpleTask("notify", TaskResult.SUCCESS, "notified", "true");

        // Configurar steps (cada step envuelve su task via StepTask)
        FluxyStep validateStep = createStep("validation", validateTask);
        FluxyStep processStep = createStep("processing", processTask);
        FluxyStep notifyStep = createStep("notification", notifyTask);

        // Configurar flow steps (flow conoce steps via FlowStep)
        FlowStep fs1 = new FlowStep(1, validateStep, null);
        FlowStep fs2 = new FlowStep(2, processStep, null);
        FlowStep fs3 = new FlowStep(3, notifyStep, null);

        // Configurar flow
        FluxyFlow flow = new FluxyFlow();
        flow.setName("order-processing");
        flow.setType("order");
        flow.setDescription("Flujo de procesamiento de orden");
        flow.setSteps(List.of(fs1, fs2, fs3));
        flow.setConnections(new ArrayList<>());

        // Crear contexto de ejecución
        ExecutionContext context = new ExecutionContext("order-processing", "1.0");
        context.addParameter("orderId", "ORD-001");
        context.addReference("customer", "CUST-123");

        // Crear ejecución
        FluxyExecution execution = new FluxyExecution(flow, context);

        // Inicializar ejecución
        flowExecutor.initializeExecution(execution);

        // Verificar estado inicial
        assertEquals(StepStatus.PENDING, fs1.getStepStatus());
        assertEquals(StepStatus.PENDING, fs2.getStepStatus());
        assertEquals(StepStatus.PENDING, fs3.getStepStatus());

        // === Ejecutar step 1 ===
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs1.getStepStatus());
        assertEquals(StepStatus.PENDING, fs2.getStepStatus());
        assertEquals(StepStatus.PENDING, fs3.getStepStatus());
        assertEquals("true", context.getVariable("validated").orElse(null));
        assertEquals(1, validateTask.getExecutionCount());

        // === Ejecutar step 2 ===
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs2.getStepStatus());
        assertEquals(StepStatus.PENDING, fs3.getStepStatus());
        assertEquals("true", context.getVariable("processed").orElse(null));
        assertEquals(1, processTask.getExecutionCount());

        // === Ejecutar step 3 ===
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs3.getStepStatus());
        assertEquals("true", context.getVariable("notified").orElse(null));
        assertEquals(1, notifyTask.getExecutionCount());

        // Verificar traza completa
        assertEquals(3, eventsBus.getPublishedEvents().size());

        // Verificar que el contexto conserva toda la información
        assertEquals("ORD-001", context.getVariable("orderId").orElse(null));
        assertEquals("CUST-123", context.getReference("customer").orElse(null));
    }

    // =========================================================================
    // Test 2: Flujo lineal con step multi-task
    // =========================================================================

    @Test
    void testLinearFlowExecution_stepWithMultipleTasks() {
        SimpleTask fetchTask = new SimpleTask("fetch-data", TaskResult.SUCCESS, "dataFetched", "true");
        SimpleTask transformTask = new SimpleTask("transform-data", TaskResult.SUCCESS, "dataTransformed", "true");
        SimpleTask saveTask = new SimpleTask("save-result", TaskResult.SUCCESS, "saved", "true");

        // Un step con 2 tasks internas
        FluxyStep etlStep = createStep("etl", fetchTask, transformTask);
        FluxyStep saveStep = createStep("save", saveTask);

        FlowStep fs1 = new FlowStep(1, etlStep, null);
        FlowStep fs2 = new FlowStep(2, saveStep, null);

        FluxyFlow flow = new FluxyFlow();
        flow.setName("etl-flow");
        flow.setSteps(List.of(fs1, fs2));
        flow.setConnections(new ArrayList<>());

        ExecutionContext context = createContext("etl-flow");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Primera llamada: ejecuta task1 del step etl — step queda RUNNING
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.RUNNING, fs1.getStepStatus());
        assertEquals(TaskStatus.FINISHED, etlStep.getTasks().get(0).getStatus());
        assertEquals(TaskStatus.PENDING, etlStep.getTasks().get(1).getStatus());

        // Segunda llamada: ejecuta task2 del step etl — step se completa
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs1.getStepStatus());
        assertEquals(TaskStatus.FINISHED, etlStep.getTasks().get(1).getStatus());

        // Tercera llamada: ejecuta save step
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs2.getStepStatus());

        // Verificar contexto
        assertEquals("true", context.getVariable("dataFetched").orElse(null));
        assertEquals("true", context.getVariable("dataTransformed").orElse(null));
        assertEquals("true", context.getVariable("saved").orElse(null));
    }

    // =========================================================================
    // Test 3: Flujo con bifurcación — dos ramas, condición verdadera
    // =========================================================================

    @Test
    void testBranchingFlowExecution_conditionTrue() {
        // Step 1: Evaluación que determina la ruta
        SimpleTask evaluateTask = new SimpleTask("evaluate", TaskResult.SUCCESS, "priority", "high");

        // Rama A: Alta prioridad
        SimpleTask urgentTask = new SimpleTask("urgent-process", TaskResult.SUCCESS, "urgentDone", "true");

        // Rama B: Baja prioridad
        SimpleTask normalTask = new SimpleTask("normal-process", TaskResult.SUCCESS, "normalDone", "true");

        // Step final
        SimpleTask completeTask = new SimpleTask("complete", TaskResult.SUCCESS, "completed", "true");

        FluxyStep evalStep = createStep("evaluate", evaluateTask);
        FluxyStep urgentStep = createStep("urgent", urgentTask);
        FluxyStep normalStep = createStep("normal", normalTask);
        FluxyStep completeStep = createStep("complete", completeTask);

        FlowStep fs1 = new FlowStep(1, evalStep, null);
        FlowStep fsUrgent = new FlowStep(2, urgentStep, null);
        FlowStep fsNormal = new FlowStep(3, normalStep, null);
        FlowStep fsFinal = new FlowStep(4, completeStep, null);

        // Conexión: si priority == "high" → ir a urgentStep
        Connection highPriorityConn = new Connection(fs1, fsUrgent,
                List.of(new Condition(StandardOperator.EQ, "high", "priority")));

        // Conexión: desde urgentStep → ir directo a completeStep (saltar normalStep)
        Connection urgentToComplete = new Connection(fsUrgent, fsFinal, null);

        FluxyFlow flow = new FluxyFlow();
        flow.setName("priority-routing");
        flow.setSteps(List.of(fs1, fsUrgent, fsNormal, fsFinal));
        flow.setConnections(List.of(highPriorityConn, urgentToComplete));

        ExecutionContext context = createContext("priority-routing");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Ejecuta evaluate → agrega priority=high
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs1.getStepStatus());
        assertEquals("high", context.getVariableByPath("priority"));

        // Bifurcación: priority == "high" → va a urgentStep
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fsUrgent.getStepStatus());
        assertEquals(StepStatus.PENDING, fsNormal.getStepStatus()); // normal NO se ejecutó

        // Desde urgentStep → va directo a completeStep (conexión sin condición)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fsFinal.getStepStatus());
        assertEquals(StepStatus.PENDING, fsNormal.getStepStatus()); // normal sigue sin ejecutarse

        // Verificar que solo se ejecutaron las tasks correctas
        assertEquals(1, evaluateTask.getExecutionCount());
        assertEquals(1, urgentTask.getExecutionCount());
        assertEquals(0, normalTask.getExecutionCount()); // ¡No se ejecutó!
        assertEquals(1, completeTask.getExecutionCount());
    }

    // =========================================================================
    // Test 4: Flujo con bifurcación — condición no cumplida, cae al secuencial
    // =========================================================================

    @Test
    void testBranchingFlowExecution_conditionFalse_fallsBackToSequential() {
        SimpleTask evaluateTask = new SimpleTask("evaluate", TaskResult.SUCCESS, "priority", "low");
        SimpleTask urgentTask = new SimpleTask("urgent-process", TaskResult.SUCCESS);
        SimpleTask normalTask = new SimpleTask("normal-process", TaskResult.SUCCESS, "normalDone", "true");

        FluxyStep evalStep = createStep("evaluate", evaluateTask);
        FluxyStep urgentStep = createStep("urgent", urgentTask);
        FluxyStep normalStep = createStep("normal", normalTask);

        FlowStep fs1 = new FlowStep(1, evalStep, null);
        FlowStep fsUrgent = new FlowStep(2, urgentStep, null);
        FlowStep fsNormal = new FlowStep(3, normalStep, null);

        // Conexión: si priority == "high" → urgentStep — NO se cumplirá
        Connection highPriorityConn = new Connection(fs1, fsUrgent,
                List.of(new Condition(StandardOperator.EQ, "high", "priority")));

        FluxyFlow flow = new FluxyFlow();
        flow.setName("priority-routing");
        flow.setSteps(List.of(fs1, fsUrgent, fsNormal));
        flow.setConnections(List.of(highPriorityConn));

        ExecutionContext context = createContext("priority-routing");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        // Ejecuta evaluate → priority=low
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs1.getStepStatus());

        // Condición NO se cumple → cae al siguiente PENDING en orden → urgentStep (orden 2)
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fsUrgent.getStepStatus());

        // Siguiente PENDING → normalStep
        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fsNormal.getStepStatus());
    }

    // =========================================================================
    // Test 5: Verifica la traza de ejecución en ExecutionMetaInf
    // =========================================================================

    @Test
    void testExecutionMetaInfTraceIsComplete() {
        SimpleTask task1 = new SimpleTask("task1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("task2", TaskResult.FAILURE);

        FluxyStep step1 = createStep("step1", task1);
        FluxyStep step2 = createStep("step2", task2);

        FlowStep fs1 = new FlowStep(1, step1, null);
        FlowStep fs2 = new FlowStep(2, step2, null);

        FluxyFlow flow = new FluxyFlow();
        flow.setName("trace-flow");
        flow.setSteps(List.of(fs1, fs2));
        flow.setConnections(new ArrayList<>());

        ExecutionContext context = createContext("trace-flow");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        flowExecutor.processFlow(execution);
        flowExecutor.processFlow(execution);

        // Verificar ExecutionMetaInf contiene todos los steps
        Map<FlowStep, List<StepTask>> executionMap = execution.getMetaInf().getExecution();
        assertEquals(2, executionMap.size());

        // Verificar traza del step 1
        List<StepTask> step1Tasks = executionMap.get(fs1);
        assertNotNull(step1Tasks);
        assertEquals(1, step1Tasks.size());
        assertEquals(TaskStatus.FINISHED, step1Tasks.getFirst().getStatus());
        assertEquals(TaskResult.SUCCESS, step1Tasks.getFirst().getResult());

        // Verificar traza del step 2
        List<StepTask> step2Tasks = executionMap.get(fs2);
        assertNotNull(step2Tasks);
        assertEquals(1, step2Tasks.size());
        assertEquals(TaskStatus.FINISHED, step2Tasks.getFirst().getStatus());
        assertEquals(TaskResult.FAILURE, step2Tasks.getFirst().getResult());

        // Todos los eventos publicados
        assertEquals(2, eventsBus.getPublishedEvents().size());
    }

    // =========================================================================
    // Test 6: Reutilización — mismo step en múltiples flows
    // =========================================================================

    @Test
    void testStepReusabilityAcrossFlows() {
        // La misma task y step son reutilizados por dos flows distintos
        SimpleTask sharedTask = new SimpleTask("shared-validate", TaskResult.SUCCESS, "validated", "true");
        SimpleTask flowATask = new SimpleTask("flowA-action", TaskResult.SUCCESS, "flowA", "done");
        SimpleTask flowBTask = new SimpleTask("flowB-action", TaskResult.SUCCESS, "flowB", "done");

        // Step compartido (será reutilizado)
        FluxyStep sharedStep = createStep("shared-validation", sharedTask);

        // Steps específicos de cada flow
        FluxyStep flowAStep = createStep("flowA-step", flowATask);
        FluxyStep flowBStep = createStep("flowB-step", flowBTask);

        // Flow A usa sharedStep + flowAStep
        FlowStep fsA1 = new FlowStep(1, sharedStep, null);
        FlowStep fsA2 = new FlowStep(2, flowAStep, null);
        FluxyFlow flowA = new FluxyFlow();
        flowA.setName("flow-A");
        flowA.setSteps(List.of(fsA1, fsA2));
        flowA.setConnections(new ArrayList<>());

        // Flow B usa sharedStep + flowBStep
        FlowStep fsB1 = new FlowStep(1, sharedStep, null);
        FlowStep fsB2 = new FlowStep(2, flowBStep, null);
        FluxyFlow flowB = new FluxyFlow();
        flowB.setName("flow-B");
        flowB.setSteps(List.of(fsB1, fsB2));
        flowB.setConnections(new ArrayList<>());

        // Ejecutar Flow A con su propio contexto
        ExecutionContext contextA = new ExecutionContext("flow-A", "1.0");
        contextA.addParameter("source", "A");
        contextA.addReference("correlationA", "A-001");
        FluxyExecution executionA = new FluxyExecution(flowA, contextA);
        flowExecutor.initializeExecution(executionA);
        flowExecutor.processFlow(executionA);
        flowExecutor.processFlow(executionA);

        assertEquals(StepStatus.FINISHED, fsA1.getStepStatus());
        assertEquals(StepStatus.FINISHED, fsA2.getStepStatus());
        assertEquals("done", contextA.getVariable("flowA").orElse(null));

        // Ejecutar Flow B con su propio contexto (diferente)
        eventsBus.clear();
        ExecutionContext contextB = new ExecutionContext("flow-B", "1.0");
        contextB.addParameter("source", "B");
        contextB.addReference("correlationB", "B-001");
        FluxyExecution executionB = new FluxyExecution(flowB, contextB);
        flowExecutor.initializeExecution(executionB);
        flowExecutor.processFlow(executionB);
        flowExecutor.processFlow(executionB);

        assertEquals(StepStatus.FINISHED, fsB1.getStepStatus());
        assertEquals(StepStatus.FINISHED, fsB2.getStepStatus());
        assertEquals("done", contextB.getVariable("flowB").orElse(null));

        // Los contextos son independientes
        assertFalse(contextA.getVariable("flowB").isPresent());
        assertFalse(contextB.getVariable("flowA").isPresent());
    }

    // =========================================================================
    // Test 7: Verifica que flow no se termina si queda un paso que no existe
    // =========================================================================

    @Test
    void testFlowCompletionThrowsWhenNoMoreSteps() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        FluxyStep step = createStep("step1", task);
        FlowStep fs = new FlowStep(1, step, null);

        FluxyFlow flow = new FluxyFlow();
        flow.setName("single-step-flow");
        flow.setSteps(List.of(fs));
        flow.setConnections(new ArrayList<>());

        ExecutionContext context = createContext("single-step-flow");
        FluxyExecution execution = new FluxyExecution(flow, context);
        flowExecutor.initializeExecution(execution);

        flowExecutor.processFlow(execution);
        assertEquals(StepStatus.FINISHED, fs.getStepStatus());

        // Intentar continuar cuando ya terminó
        assertThrows(IllegalStateException.class,
                () -> flowExecutor.processFlow(execution));
    }
}

