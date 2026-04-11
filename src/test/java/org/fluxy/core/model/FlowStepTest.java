package org.fluxy.core.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FlowStepTest {

    @Test
    void testConstruction() {
        FluxyStep step = new FluxyStep();
        step.setName("step1");

        FlowStep flowStep = new FlowStep(1, step, StepStatus.PENDING);

        assertEquals(1, flowStep.getOrder());
        assertEquals(step, flowStep.getStep());
        assertEquals(StepStatus.PENDING, flowStep.getStepStatus());
    }

    @Test
    void testStepStatusChange() {
        FluxyStep step = new FluxyStep();
        step.setName("step1");

        FlowStep flowStep = new FlowStep(1, step, StepStatus.PENDING);
        assertEquals(StepStatus.PENDING, flowStep.getStepStatus());

        flowStep.setStepStatus(StepStatus.RUNNING);
        assertEquals(StepStatus.RUNNING, flowStep.getStepStatus());

        flowStep.setStepStatus(StepStatus.FINISHED);
        assertEquals(StepStatus.FINISHED, flowStep.getStepStatus());
    }

    @Test
    void testEqualitySameOrderAndStep() {
        FluxyStep step = new FluxyStep();
        step.setName("step1");

        FlowStep flowStep1 = new FlowStep(1, step, StepStatus.PENDING);
        FlowStep flowStep2 = new FlowStep(1, step, StepStatus.RUNNING);

        // equals/hashCode se basan en order y step, no en stepStatus
        assertEquals(flowStep1, flowStep2);
        assertEquals(flowStep1.hashCode(), flowStep2.hashCode());
    }

    @Test
    void testInequalityDifferentOrder() {
        FluxyStep step = new FluxyStep();
        step.setName("step1");

        FlowStep flowStep1 = new FlowStep(1, step, StepStatus.PENDING);
        FlowStep flowStep2 = new FlowStep(2, step, StepStatus.PENDING);

        assertNotEquals(flowStep1, flowStep2);
    }

    @Test
    void testInequalityDifferentStep() {
        FluxyStep step1 = new FluxyStep();
        step1.setName("stepA");
        FluxyStep step2 = new FluxyStep();
        step2.setName("stepB");

        FlowStep flowStep1 = new FlowStep(1, step1, StepStatus.PENDING);
        FlowStep flowStep2 = new FlowStep(1, step2, StepStatus.PENDING);

        assertNotEquals(flowStep1, flowStep2);
    }

    @Test
    void testFluxyStepEqualityByName() {
        FluxyStep step1 = new FluxyStep();
        step1.setName("same-name");
        step1.setTasks(Arrays.asList());

        FluxyStep step2 = new FluxyStep();
        step2.setName("same-name");
        step2.setTasks(null);

        // equals se basa solo en name
        assertEquals(step1, step2);
        assertEquals(step1.hashCode(), step2.hashCode());
    }

    @Test
    void testFluxyStepInequalityByName() {
        FluxyStep step1 = new FluxyStep();
        step1.setName("name-a");

        FluxyStep step2 = new FluxyStep();
        step2.setName("name-b");

        assertNotEquals(step1, step2);
    }
}

