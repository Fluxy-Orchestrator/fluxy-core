package org.fluxy.core.model;
import lombok.Getter;
/**
 * Clase base abstracta para todas las tareas de Fluxy.
 *
 * <p>Cada implementacion concreta debe definir {@code name}, {@code version}
 * y {@code description}, que se utilizan tanto para el registro en memoria
 * como para la persistencia en base de datos.</p>
 */
@Getter
public abstract class FluxyTask {
    protected String name;
    protected int version;
    protected String description;
    public abstract TaskResult execute(ExecutionContext executionContext);
}