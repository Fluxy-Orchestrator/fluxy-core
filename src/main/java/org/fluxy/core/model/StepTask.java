package org.fluxy.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class StepTask {

    private final Integer order;
    private final FluxyTask task;
    @Setter
    private TaskStatus status;
    @Setter
    private TaskResult result;

    public StepTask(Integer order, FluxyTask task) {
        this.order = order;
        this.task = task;
        this.status = TaskStatus.PENDING;
    }
}
