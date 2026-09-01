package mx.bastekor.flowweaver.model;

import lombok.Getter;
import lombok.Setter;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.util.Util;

import java.time.Instant;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

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

    @Setter
    @Getter
    private BusinessLog businessLog;
    @Setter
    @Getter
    private String exitSignature;
    @Setter
    @Getter
    private StatusEnum status;
    @Setter
    @Getter
    private String response;

    public BusinessLogContainer(String correlationId, String group, String code) {
        this.instant = now();
        this.id = randomUUID().toString();
        this.group = group;
        this.code = code;
        this.correlationId = correlationId;
    }

    public String getDuration() {
        return Util.getDuration(instant, now());
    }
}