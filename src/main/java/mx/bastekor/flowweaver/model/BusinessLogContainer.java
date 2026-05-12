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

    private final Instant instant;

    @Getter
    private final String id;
    @Getter
    private final String code;
    @Getter
    private final String group;
    @Getter
    private final String correlationId;

    @Setter @Getter
    private BusinessLog businessLog;
    @Setter @Getter
    private String exitSignature;
    @Setter @Getter
    private StatusEnum status;
    @Setter @Getter
    private Object response;
    private final List<AuditTrailContainer> auditTrails;

    public BusinessLogContainer(String group, String code) {
        this.instant = now();
        this.id = randomUUID().toString();
        this.code = code;
        this.group = group;
        this.correlationId = randomUUID().toString();
        this.auditTrails = new ArrayList<>();
    }

    public String getDuration() {
        return Util.getDuration(instant, now());
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
}