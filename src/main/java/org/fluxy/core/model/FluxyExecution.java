package org.fluxy.core.model;
import lombok.Getter;
import lombok.Setter;
/**
 * Representa una instancia de ejecucion de un {@link FluxyFlow} con un
 * {@link ExecutionContext} especifico.
 *
 * <p>Es la unidad central que aisla el estado de cada corrida de un flow.
 * Un mismo flow puede tener multiples ejecuciones simultaneas, cada una
 * con su propio contexto y su propia traza ({@link ExecutionMetaInf}).</p>
 *
 * <h3>Invariantes</h3>
 * <ul>
 *   <li>El nombre del flow debe coincidir con el tipo del contexto.</li>
 *   <li>El contexto debe contener al menos una referencia.</li>
 *   <li>El estado inicial siempre es {@link ExecutionStatus#PENDING}.</li>
 * </ul>
 */
@Getter
public class FluxyExecution {
    private final FluxyFlow flow;
    private final ExecutionContext context;
    private final ExecutionMetaInf metaInf;
    @Setter
    private ExecutionStatus status;
    /**
     * Crea una nueva ejecucion validando los invariantes del sistema.
     *
     * @param flow    definicion del flujo a ejecutar
     * @param context contexto de ejecucion con variables y referencias de negocio
     * @throws IllegalArgumentException si el tipo del contexto no coincide con el nombre del flow
     * @throws IllegalArgumentException si el contexto no contiene referencias
     */
    public FluxyExecution(FluxyFlow flow, ExecutionContext context) {
        if (flow == null || context == null) {
            throw new IllegalArgumentException(
                    "El flow y el contexto son obligatorios para crear una ejecucion.");
        }
        if (!flow.getName().equals(context.getType())) {
            throw new IllegalArgumentException(
                    ("El flow '%s' no es compatible con un contexto de tipo '%s'. " +
                     "El nombre del flow y el tipo del contexto deben coincidir.")
                            .formatted(flow.getName(), context.getType()));
        }
        if (context.getReferences() == null || context.getReferences().isEmpty()) {
            throw new IllegalArgumentException(
                    ("El contexto debe contener al menos una referencia al " +
                     "inicializar una ejecucion del flow '%s'.")
                            .formatted(flow.getName()));
        }
        this.flow = flow;
        this.context = context;
        this.metaInf = new ExecutionMetaInf();
        this.status = ExecutionStatus.PENDING;
    }
}
