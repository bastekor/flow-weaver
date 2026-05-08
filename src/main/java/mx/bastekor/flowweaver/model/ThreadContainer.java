package mx.bastekor.flowweaver.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_PREFIX;

public class ThreadContainer {

    @Getter
    private final String threadId;
    @Getter
    private final String threadName;
    private final Map<String, BusinessLogContainer> businessLogs;

    public ThreadContainer() {
        this.threadId = UUID.randomUUID().toString();
        this.threadName = Thread.currentThread().getName();
        this.businessLogs = new ConcurrentHashMap<>();
    }

    public void addBusinessLogContainer(final BusinessLogContainer businessLogContainer) {
        // Ahora guardamos por flowId (no por operationCode), con esto evitamos eliminar o sobreescribir aquellos existentes
        businessLogs.put(businessLogContainer.getOperationId(), businessLogContainer);
    }

    /**
     * Obtiene el objeto {@link BusinessLogContainer} mediante el {@code operationCode} recibido.
     * Ahora busca entre los values y devuelve el más "reciente" (último creado) si existen varios.
     */
    public Optional<BusinessLogContainer> getBusinessLogContainer(final String operationCode) {
        // Buscar el último BusinessLogContainer con ese operationCode.
        // Si hay varios, devolvemos el que tenga la fecha/orden más reciente (lo último insertado).
        return businessLogs.values()
                .stream()
                .filter(bl -> operationCode.equals(bl.getOperationCode()))
                // ordenar por start (si lo expones) o por flowId no es fiable; mejor tomar el último encontrado:
                .reduce((first, second) -> second); // devuelve el último del stream
    }

    /**
     * Método encargado de buscar el {@code BusinessLogContainer} por defecto que es el
     * único que inicia con el {@code operationCode} por el prefijo "BL#".
     */
    public Optional<BusinessLogContainer> getBusinessLogContainerDefault() {
        return businessLogs.values()
                .stream()
                .filter(bl -> bl.getOperationCode().startsWith(BUSINESS_LOG_PREFIX))
                .findFirst();
    }

    public List<BusinessLogContainer> getAllBusinessLogContainer() {
        return List.copyOf(businessLogs.values());
    }

    /**
     * Borra el BusinessLogContainer por operationCode (compatibilidad con API actual).
     * Si hay varios con el mismo operationCode elimina el último (el más "reciente").
     */
    public void clearBusinessLogContainer(final String operationCode) {
        // Encontrar el flowId del último que coincida y eliminarlo por flowId.
        Optional<String> keyToRemove = businessLogs.entrySet()
                .stream()
                .filter(e -> operationCode.equals(e.getValue().getOperationCode()))
                .map(Map.Entry::getKey)
                .reduce((first, second) -> second); // el último
        keyToRemove.ifPresent(businessLogs::remove);
    }
}
