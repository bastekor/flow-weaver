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
    private final String id;
    @Getter
    private final String name;
    @Getter
    private final String correlationId;
    private final Map<String, BusinessLogContainer> businessLogs;

    public ThreadContainer() {
        this.id = UUID.randomUUID().toString();
        this.name = Thread.currentThread().getName();
        this.correlationId = UUID.randomUUID().toString();
        this.businessLogs = new ConcurrentHashMap<>();
    }

    public void addBusinessLogContainer(final BusinessLogContainer businessLogContainer) {
        businessLogs.put(businessLogContainer.getId(), businessLogContainer);
    }

    /**
     * Obtiene el objeto {@link BusinessLogContainer} mediante el {@code code} recibido.
     * Ahora busca entre los values y devuelve el más "reciente" (último creado) si existen varios.
     */
    public Optional<BusinessLogContainer> getBusinessLogContainer(final String code) {
        return businessLogs.values()
                .stream()
                .filter(bl -> code.equals(bl.getCode()))
                .findFirst();
    }

    /**
     * Método encargado de buscar el {@code BusinessLogContainer} por defecto que es el
     * único que inicia con el {@code operationCode} por el prefijo "BL#".
     */
    public Optional<BusinessLogContainer> getBusinessLogContainerDefault() {
        return businessLogs.values()
                .stream()
                .filter(bl -> bl.getCode().startsWith(BUSINESS_LOG_PREFIX))
                .reduce((first, second) -> second);
    }

    public List<BusinessLogContainer> getAllBusinessLogContainer() {
        return List.copyOf(businessLogs.values());
    }

    /**
     * Borra el BusinessLogContainer por code (compatibilidad con API actual).
     * Si hay varios con el mismo code elimina el último (el más "reciente").
     */
    public void clearBusinessLogContainer(final String code) {
        Optional<String> keyToRemove = businessLogs.entrySet()
                .stream()
                .filter(e -> code.equals(e.getValue().getCode()))
                .map(Map.Entry::getKey)
                .reduce((first, second) -> second); // el último
        keyToRemove.ifPresent(businessLogs::remove);
    }
}
