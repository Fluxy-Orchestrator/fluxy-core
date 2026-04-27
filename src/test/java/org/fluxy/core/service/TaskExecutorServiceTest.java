package org.fluxy.core.service;

import org.fluxy.core.exception.TaskExecutionException;
import org.fluxy.core.model.*;
import org.fluxy.core.support.FailingTask;
import org.fluxy.core.support.InMemoryFluxyEventsBus;
import org.fluxy.core.support.SimpleExecutionContext;
import org.fluxy.core.support.SimpleTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskExecutorServiceTest {

    private InMemoryFluxyEventsBus eventsBus;
    private TaskExecutorService taskExecutorService;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        eventsBus = new InMemoryFluxyEventsBus();
        taskExecutorService = new TaskExecutorService(eventsBus);
        context = new SimpleExecutionContext("test", "1.0");
    }

    @Test
    void testExecuteTaskSuccess() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);

        TaskResult result = taskExecutorService.executeTask(task, context);

        assertEquals(TaskResult.SUCCESS, result);
        assertEquals(1, task.getExecutionCount());
    }

    @Test
    void testExecuteTaskFailure() {
        SimpleTask task = new SimpleTask("task1", TaskResult.FAILURE);

        TaskResult result = taskExecutorService.executeTask(task, context);

        assertEquals(TaskResult.FAILURE, result);
        assertEquals(1, task.getExecutionCount());
    }

    @Test
    void testExecuteTaskPublishesEventOnSuccess() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS);

        taskExecutorService.executeTask(task, context);

        assertEquals(1, eventsBus.getPublishedEvents().size());
        FluxyEvent<?, ?> event = eventsBus.getPublishedEvents().getFirst();
        assertSame(task, event.getSource());
        assertEquals(TaskResult.SUCCESS, event.getPayload());
        assertSame(context, event.getContext());
    }

    @Test
    void testExecuteTaskPublishesEventOnFailureResult() {
        SimpleTask task = new SimpleTask("task1", TaskResult.FAILURE);

        taskExecutorService.executeTask(task, context);

        assertEquals(1, eventsBus.getPublishedEvents().size());
        FluxyEvent<?, ?> event = eventsBus.getPublishedEvents().getFirst();
        assertEquals(TaskResult.FAILURE, event.getPayload());
    }

    @Test
    void testExecuteTaskExceptionThrowsTaskExecutionException() {
        RuntimeException cause = new RuntimeException("boom");
        FailingTask task = new FailingTask("failing-task", cause);

        TaskExecutionException thrown = assertThrows(
                TaskExecutionException.class,
                () -> taskExecutorService.executeTask(task, context)
        );

        assertTrue(thrown.getMessage().contains("failing-task"));
        assertSame(cause, thrown.getCause());
    }

    @Test
    void testExecuteTaskExceptionPublishesEventWithException() {
        RuntimeException cause = new RuntimeException("boom");
        FailingTask task = new FailingTask("failing-task", cause);

        assertThrows(TaskExecutionException.class,
                () -> taskExecutorService.executeTask(task, context));

        assertEquals(1, eventsBus.getPublishedEvents().size());
        FluxyEvent<?, ?> event = eventsBus.getPublishedEvents().getFirst();
        assertSame(task, event.getSource());
        assertSame(cause, event.getPayload());
        assertSame(context, event.getContext());
    }

    @Test
    void testExecuteTaskModifiesContext() {
        SimpleTask task = new SimpleTask("task1", TaskResult.SUCCESS, "output", "result-value");

        taskExecutorService.executeTask(task, context);

        assertEquals("result-value", context.getVariable("output").orElse(null));
    }
}

