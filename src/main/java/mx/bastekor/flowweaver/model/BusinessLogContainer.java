package mx.bastekor.flowweaver.model;

import java.time.Instant;
import static java.time.Instant.now;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static java.util.UUID.randomUUID;

import lombok.Getter;
import lombok.Setter;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.util.Util;

public class BusinessLogContainer {

    @Getter
    private final String operationId;
    @Getter
    private final String operationCode;
    @Getter
    private final String groupCode;
    private final Instant start;

    @Setter @Getter
    private String exitSignature;
    @Setter @Getter
    private BusinessLog businessLog;
    @Setter @Getter
    private StatusEnum status;
    @Setter @Getter
    private Object response;
    private final List<AuditTrailContainer> auditTrails;

    public BusinessLogContainer(String groupCode, String operationCode) {
        this.groupCode = groupCode;
        this.operationCode = operationCode;
        this.operationId = randomUUID().toString();
        this.start = now();
        this.auditTrails = new ArrayList<>();
    }

    public String getDuration() {
        return Util.getDuration(start, now());
    }

    public void addAuditTrail(AuditTrailContainer auditTrail) {
        auditTrails.add(auditTrail);
    }

    /**
     * Obtener todos los AuditTrails (inmutable)
     */
    public List<AuditTrailContainer> getAuditTrails() {
        return Collections.unmodifiableList(auditTrails);
    }

    /**
     * Obtener cantidad de AuditTrails registrados
     */
    public int getAuditTrailCount() {
        return auditTrails.size();
    }

    public int getB() {
        return 0;
    }
}