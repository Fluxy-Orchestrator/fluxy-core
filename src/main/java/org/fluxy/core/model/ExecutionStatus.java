package org.fluxy.core.model;

/**
 * Estado del ciclo de vida de una {@link FluxyExecution}.
 *
 * <ul>
 *   <li>{@code PENDING} — Ejecución inicializada, ningún step ha sido procesado aún.</li>
 *   <li>{@code RUNNING} — Al menos un step ha comenzado a ejecutarse.</li>
 *   <li>{@code FINISHED} — Todos los steps han completado su ejecución.</li>
 * </ul>
 */
public enum ExecutionStatus {
    PENDING,
    RUNNING,
    FINISHED
}

