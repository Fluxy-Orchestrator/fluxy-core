package org.fluxy.core.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(of = "name")
public class FluxyStep {

    private String name;
    private List<StepTask> tasks;


}
