package org.fluxy.core.model;

import lombok.Getter;

@Getter
public abstract class FluxyTask {

    protected String name;

    public abstract TaskResult execute(ExecutionContext executionContext);

}
