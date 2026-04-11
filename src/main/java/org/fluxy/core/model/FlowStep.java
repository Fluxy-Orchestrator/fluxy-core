package org.fluxy.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"order", "step"})
public class FlowStep {
    private Integer order;
    private FluxyStep step;
    private StepStatus stepStatus;
}