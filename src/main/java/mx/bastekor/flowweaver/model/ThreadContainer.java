package mx.bastekor.flowweaver.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public void addBusinessLogContainer(BusinessLogContainer businessLogContainer) {
        businessLogs.put(businessLogContainer.getOperationCode(), businessLogContainer);
    }

    /**
     * Obtiene el objeto {@link BusinessLogContainer} mediante el {@code operationCode} recibido.
     * @param operationCode Valor por el cual se buscará el objeto.
     * @return {@link BusinessLogContainer} si es que existe.
     */
    public BusinessLogContainer getBusinessLogContainer(String operationCode) {
        return businessLogs.get(operationCode);
    }

    /**
     * Método encargado de buscar el {@code BusinessLogContainer} por defecto que es el
     * único que inicia con el {@code operationCode} por el prefijo "BL#".
     * @return BusinessLogContainer
     */
    public BusinessLogContainer getBusinessLogContainerDefault() {
        return businessLogs.values()
                .stream()
                .filter(bl -> bl.getOperationCode().startsWith("BL#"))
                .findFirst()
                .orElse(null);
    }

    public List<BusinessLogContainer> getBusinessLogs() {
        return List.copyOf(businessLogs.values());
    }

    public void clearBusinessLogContainer(String operationCode) {
        businessLogs.remove(operationCode);
    }
}