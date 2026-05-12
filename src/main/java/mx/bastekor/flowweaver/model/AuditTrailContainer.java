package mx.bastekor.flowweaver.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.util.Util;

import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;

@ToString
public class AuditTrailContainer {

    private final Instant instant;

    @Getter
    private final String id;
    @Setter @Getter
    private String code;
    @Setter @Getter
    private String group;
    @Setter @Getter
    private String correlationId;
    @Setter @Getter
    private String parentCode;

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
        this.instant = now();
        this.id = UUID.randomUUID().toString();
    }

    public String getDuration() {
        return Util.getDuration(instant, now());
    }
}