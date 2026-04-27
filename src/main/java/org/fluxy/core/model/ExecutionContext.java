package org.fluxy.core.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contrato base del contexto de ejecución.
 *
 * <p>Provee la gestión de {@link Variable} y {@link Reference} como pares
 * nombre-valor en formato String. Las operaciones tipadas que requieren
 * serialización/deserialización se declaran como contrato abstracto
 * y son implementadas por subclases concretas (e.g. {@code ExecutionContextProxy}
 * usando Jackson en el starter de Spring).</p>
 *
 * <p>Los usuarios que no utilicen el starter de Spring pueden crear su propia
 * subclase e implementar los métodos abstractos con la tecnología de
 * serialización que prefieran.</p>
 */
@Data
public abstract class ExecutionContext {

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

    // ── Métodos tipados (contrato abstracto — implementados por subclases) ──────

    /**
     * Obtiene una variable del contexto y la deserializa al tipo indicado.
     *
     * @param name nombre de la variable
     * @param type clase destino de la deserialización
     * @throws org.fluxy.core.exception.UndefinedContextVariableException si la variable no existe
     * @throws org.fluxy.core.exception.ContextVariableCastException si la deserialización falla
     */
    public abstract <T> T getVariable(String name, Class<T> type);

    /**
     * Obtiene una referencia del contexto y la deserializa al tipo indicado.
     *
     * @param type  tipo de la referencia
     * @param clazz clase destino de la deserialización
     * @throws org.fluxy.core.exception.UndefinedContextReferenceException si la referencia no existe
     * @throws org.fluxy.core.exception.ContextReferenceCastException si la deserialización falla
     */
    public abstract <T> T getReference(String type, Class<T> clazz);

    /**
     * Serializa {@code value} y lo almacena como variable con el nombre indicado.
     *
     * @param name  nombre de la variable
     * @param value objeto a serializar
     */
    public abstract void addVariable(String name, Object value);

    /**
     * Serializa {@code value} y lo almacena como referencia con el tipo indicado.
     *
     * @param type  tipo de la referencia (debe ser único en el contexto)
     * @param value objeto a serializar
     * @throws IllegalArgumentException si ya existe una referencia con ese tipo
     */
    public abstract void addReference(String type, Object value);
}
