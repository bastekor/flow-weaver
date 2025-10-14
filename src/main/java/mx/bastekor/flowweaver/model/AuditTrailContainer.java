package mx.bastekor.flowweaver.model;

import java.time.Instant;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import lombok.Getter;
import mx.bastekor.flowweaver.util.Util;

public class AuditTrailContainer {

    @Getter
    private final String flowCode;
    @Getter
    private final String operationCode;
    @Getter
    private final String flowId;
    private final Instant start;

    public AuditTrailContainer(String flowCode, String operationCode) {
        this(flowCode, operationCode, randomUUID().toString());
    }

    public AuditTrailContainer(String flowCode, String operationCode, String flowId) {
        this.flowCode = flowCode;
        this.operationCode = operationCode;
        this.flowId = flowId;
        this.start = now();
    }

    public String getDuration() {
        return Util.getDuration(start, now());
    }
}