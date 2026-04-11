package org.fluxy.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class Reference {

    private UUID id;

    @Setter
    private String type;

    @Setter
    private String value;

    public Reference(String type, String value) {
        this.type = type;
        this.value = value;
    }
}
