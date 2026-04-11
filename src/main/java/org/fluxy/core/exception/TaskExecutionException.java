package org.fluxy.core.exception;

public class TaskExecutionException extends RuntimeException {

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
