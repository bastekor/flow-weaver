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

    public BusinessLogContainer getBusinessLogContainer(String operationCode) {
        return businessLogs.get(operationCode);
    }

    public List<BusinessLogContainer> getBusinessLogs() {
        return List.copyOf(businessLogs.values());
    }

    public void clearBusinessLogContainer(String operationCode) {
        businessLogs.remove(operationCode);
    }
}