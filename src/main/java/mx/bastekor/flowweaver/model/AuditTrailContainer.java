package mx.bastekor.flowweaver.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.util.Util;

import java.time.Instant;

import static java.time.Instant.now;

@ToString
public class AuditTrailContainer {

    private final Instant start;

    @Setter @Getter
    private String flowCode;
    @Setter @Getter
    private String operationCode;
    @Setter @Getter
    private String groupCode;
    @Setter @Getter
    private String flowId;

    @Setter @Getter
    private String entrySignature; // firma de entrada del metodo
    @Setter @Getter
    private String exitSignature; // firma de salida del metodo
    @Setter @Getter
    private AuditTrail auditTrail;
    @Setter @Getter
    private StatusEnum status;
    @Setter @Getter
    private Object response;

    public AuditTrailContainer() {
        this.start = now();
    }

    public AuditTrailContainer(String groupCode, String flowCode, String flowId, String operationCode) {
        this.groupCode = groupCode;
        this.flowCode = flowCode;
        this.flowId = flowId;
        this.operationCode = operationCode;
        this.start = now();
    }

    public String getDuration() {
        return Util.getDuration(start, now());
    }
}