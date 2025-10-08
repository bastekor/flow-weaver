package mx.bastekor.flowweaver.context;

import java.time.Duration;
import java.time.Instant;
import static java.time.Instant.now;

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
     * Contador de tiempo que será inicializado cuando se instancie la clase
     */
    private final Instant start;

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
        this.start = now();
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

    /**
     * Método encargado de realizar la estimación de la duración del proceso interceptado, retornando
     * el valor en una cadena de texto similar a los siguientes resultados: ["1m 34s 657ms", "1s 245ms" o "135ms"]
     *
     * @return Duración calculada del proceso.
     */
    public String getDuration() {
        Instant end = now();
        Duration duration = Duration.between(start, end);

        long totalMillis = duration.toMillis();

        long minutes = (totalMillis / (1000 * 60)) % 60;
        long seconds = (totalMillis / 1000) % 60;
        long millis = totalMillis % 1000;

        if (minutes > 0) {
            return String.format("%dm %ds %dms", minutes, seconds, millis);
        } else if (seconds > 0) {
            return String.format("%ds %dms", seconds, millis);
        } else {
            return String.format("%dms", millis);
        }
    }
}