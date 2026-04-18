package org.fluxy.core.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class ExecutionContext {

    private List<Variable> variables;

    private List<Reference> references;

    private String type;

    private String version;

    public ExecutionContext(String type, String version) {
        variables = new ArrayList<>();
        references = new ArrayList<>();
        this.type = type;
        this.version = version;
    }

    public Optional<String> getVariable(String name) {
        return variables.stream().filter(p -> p.getName().equals(name))
                .findFirst()
                .map(Variable::getValue);
    }

    public Optional<String> getReference(String name) {
        return references.stream().filter(p -> p.getType().equals(name))
                .findFirst()
                .map(Reference::getValue);
    }

    public void addReference(Reference reference) {
        validateUniqueReferenceType(reference.getType());
        references.add(reference);
    }

    public void addReference(String type, String value) {
        validateUniqueReferenceType(type);
        references.add(new Reference(type, value));
    }

    public void addParameter(Variable variable) {
        variables.add(variable);
    }

    public void addParameter(String name, String value) {
        variables.add(new Variable(name, value));
    }

    public Object getVariableByPath(String variablePath) {
        return variables.stream()
                .filter(p -> p.getName().equals(variablePath))
                .findFirst()
                .map(Variable::getValue)
                .orElse(null);
    }

    private void validateUniqueReferenceType(String type) {
        boolean exists = references.stream().anyMatch(r -> r.getType().equals(type));
        if (exists) {
            throw new IllegalArgumentException(
                    "Ya existe una referencia de tipo '%s' en este contexto. Cada tipo de referencia debe ser único."
                            .formatted(type));
        }
    }
}
