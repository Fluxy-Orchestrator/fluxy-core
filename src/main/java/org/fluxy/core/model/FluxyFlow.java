package org.fluxy.core.model;

import lombok.Data;

import java.util.List;

@Data
public class FluxyFlow {

    private String name;
    private String type;
    private String description;
    private List<FlowStep> steps;
    private List<Connection> connections;
}
