package org.fluxy.core.support;

import org.fluxy.core.model.ExecutionContext;

/**
 * Implementación concreta mínima de {@link ExecutionContext} para uso exclusivo en tests
 * del módulo {@code fluxy-core}, donde no hay dependencia de Jackson ni del starter.
 *
 * <p>Los métodos tipados ({@code getVariable}, {@code getReference}, {@code addVariable},
 * {@code addReference(String, Object)}) lanzan {@link UnsupportedOperationException} ya que
 * los tests del core no ejercitan serialización/deserialización. La implementación real con
 * Jackson vive en {@code ExecutionContextProxy} del starter.</p>
 */
public class SimpleExecutionContext extends ExecutionContext {

    public SimpleExecutionContext(String type, String version) {
        super(type, version);
    }

    public <T> T getVariable(String name, Class<T> type) {
        throw new UnsupportedOperationException(
                "SimpleExecutionContext no soporta deserialización tipada. " +
                "Usa ExecutionContextProxy del starter para esta operación.");
    }

    public <T> T getReference(String type, Class<T> clazz) {
        throw new UnsupportedOperationException(
                "SimpleExecutionContext no soporta deserialización tipada. " +
                "Usa ExecutionContextProxy del starter para esta operación.");
    }

    public void addVariable(String name, Object value) {
        throw new UnsupportedOperationException(
                "SimpleExecutionContext no soporta serialización tipada. " +
                "Usa ExecutionContextProxy del starter para esta operación.");
    }

    public void addReference(String type, Object value) {
        throw new UnsupportedOperationException(
                "SimpleExecutionContext no soporta serialización tipada. " +
                "Usa ExecutionContextProxy del starter para esta operación.");
    }
}

