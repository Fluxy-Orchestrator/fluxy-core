package org.fluxy.core.model;

import lombok.Getter;

@Getter
public class FluxyEvent<T, M> {

    private final T source;

    private final M payload;

    private final ExecutionContext context;

    public FluxyEvent(T source, M payload, ExecutionContext context) {
        this.source = source;
        this.payload = payload;
        this.context = context;
    }
}
