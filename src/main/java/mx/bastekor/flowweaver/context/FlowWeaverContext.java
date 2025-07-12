package mx.bastekor.flowweaver.context;

import lombok.Getter;
import lombok.ToString;

/**
 * Representa el contexto de un flujo individual dentro de un hilo.
 * Este contexto es utilizado para rastrear un identificador único de flujo
 * y contar cuántas veces ha sido referenciado en una misma ejecución o hilo.
 * Usado en conjunto con {@link FlowWeaverContextHolder}.
 */
@Getter
@ToString
public class FlowWeaverContext {

    /**
     * Identificador único del flujo en curso.
     */
    private final String flowId;

    /**
     * Contador de referencias activas al contexto dentro del hilo.
     * Se incrementa al reutilizar el contexto y se decrementa al liberar.
     */
    private int refCount;

    /**
     * Crea una nueva instancia del contexto con un ID de flujo dado
     * y un contador de referencias inicializado en 1.
     *
     * @param flowId Identificador único del flujo.
     */
    public FlowWeaverContext(String flowId) {
        this.flowId = flowId;
        this.refCount = 1;
    }

    /**
     * Incrementa el contador de referencias del contexto.
     * Llamado al reutilizar un contexto ya existente.
     */
    public void incrementRef() {
        refCount++;
    }

    /**
     * Decrementa el contador de referencias del contexto.
     * Llamado al liberar un contexto. No realiza validación de límite mínimo.
     */
    public void decrementRef() {
        refCount--;
    }
}