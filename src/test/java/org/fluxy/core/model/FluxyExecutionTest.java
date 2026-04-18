package org.fluxy.core.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class FluxyExecutionTest {

    private FluxyFlow createFlow(String name) {
        FluxyFlow flow = new FluxyFlow();
        flow.setName(name);
        flow.setSteps(new ArrayList<>());
        flow.setConnections(new ArrayList<>());
        return flow;
    }

    @Test
    void testValidConstruction() {
        FluxyFlow flow = createFlow("order-flow");
        ExecutionContext context = new ExecutionContext("order-flow", "1.0");
        context.addReference("orderId", "ORD-001");

        FluxyExecution execution = new FluxyExecution(flow, context);

        assertSame(flow, execution.getFlow());
        assertSame(context, execution.getContext());
        assertNotNull(execution.getMetaInf());
        assertEquals(ExecutionStatus.PENDING, execution.getStatus());
    }

    @Test
    void testFlowNameMustMatchContextType() {
        FluxyFlow flow = createFlow("order-flow");
        ExecutionContext context = new ExecutionContext("different-type", "1.0");
        context.addReference("orderId", "ORD-001");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new FluxyExecution(flow, context));

        assertTrue(ex.getMessage().contains("order-flow"));
        assertTrue(ex.getMessage().contains("different-type"));
    }

    @Test
    void testContextMustHaveReferences() {
        FluxyFlow flow = createFlow("order-flow");
        ExecutionContext context = new ExecutionContext("order-flow", "1.0");
        // No references added

        assertThrows(IllegalArgumentException.class,
                () -> new FluxyExecution(flow, context));
    }

    @Test
    void testNullFlowThrows() {
        ExecutionContext context = new ExecutionContext("test", "1.0");
        context.addReference("ref", "val");

        assertThrows(IllegalArgumentException.class,
                () -> new FluxyExecution(null, context));
    }

    @Test
    void testNullContextThrows() {
        FluxyFlow flow = createFlow("test");

        assertThrows(IllegalArgumentException.class,
                () -> new FluxyExecution(flow, null));
    }

    @Test
    void testStatusTransition() {
        FluxyFlow flow = createFlow("test-flow");
        ExecutionContext context = new ExecutionContext("test-flow", "1.0");
        context.addReference("ref", "val");

        FluxyExecution execution = new FluxyExecution(flow, context);

        assertEquals(ExecutionStatus.PENDING, execution.getStatus());

        execution.setStatus(ExecutionStatus.RUNNING);
        assertEquals(ExecutionStatus.RUNNING, execution.getStatus());

        execution.setStatus(ExecutionStatus.FINISHED);
        assertEquals(ExecutionStatus.FINISHED, execution.getStatus());
    }
}

