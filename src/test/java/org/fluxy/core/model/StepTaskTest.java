package org.fluxy.core.model;

import org.fluxy.core.support.SimpleTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StepTaskTest {

    @Test
    void testInitialStatusIsPending() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        StepTask stepTask = new StepTask(1, task);

        assertEquals(TaskStatus.PENDING, stepTask.getStatus());
    }

    @Test
    void testConstructionWithOrderAndTask() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        StepTask stepTask = new StepTask(1, task);

        assertEquals(1, stepTask.getOrder());
        assertEquals(task, stepTask.getTask());
        assertNull(stepTask.getResult());
    }

    @Test
    void testStatusTransitions() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        StepTask stepTask = new StepTask(1, task);

        assertEquals(TaskStatus.PENDING, stepTask.getStatus());

        stepTask.setStatus(TaskStatus.RUNNING);
        assertEquals(TaskStatus.RUNNING, stepTask.getStatus());

        stepTask.setStatus(TaskStatus.FINISHED);
        assertEquals(TaskStatus.FINISHED, stepTask.getStatus());
    }

    @Test
    void testResultUpdate() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);
        StepTask stepTask = new StepTask(1, task);

        assertNull(stepTask.getResult());

        stepTask.setResult(TaskResult.SUCCESS);
        assertEquals(TaskResult.SUCCESS, stepTask.getResult());

        stepTask.setResult(TaskResult.FAILURE);
        assertEquals(TaskResult.FAILURE, stepTask.getResult());
    }

    @Test
    void testOrderPreserved() {
        SimpleTask task1 = new SimpleTask("t1", TaskResult.SUCCESS);
        SimpleTask task2 = new SimpleTask("t2", TaskResult.SUCCESS);

        StepTask st1 = new StepTask(1, task1);
        StepTask st2 = new StepTask(2, task2);

        assertTrue(st1.getOrder() < st2.getOrder());
    }
}

