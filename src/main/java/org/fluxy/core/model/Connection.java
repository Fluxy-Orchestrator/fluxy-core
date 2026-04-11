package org.fluxy.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Connection {

    private FlowStep fromStep;
    private FlowStep toStep;
    private List<Condition> conditions;


}
