package org.fluxy.core.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExecutionMetaInf {

    private Map<FlowStep, List<StepTask>> execution;

}
