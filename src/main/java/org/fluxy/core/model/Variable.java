package org.fluxy.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class Variable {

    private UUID id;

    @Setter
    private String name;

    @Setter
    private String value;

    public Variable(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
