package mx.bastekor.flowweaver.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;

import static mx.bastekor.flowweaver.mapper.DataParamMapper.createDataParamsDTO;
import static org.apache.commons.lang3.StringUtils.trim;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditTrailMapper {

    private static final String AUDIT_TRAIL_PREFIX = "AT#";

    public static AuditTrailDTO createAuditTrailDTO(AuditTrail auditTrail) {
        if (auditTrail == null) {
            return null;
        }

        final AuditTrailDTO auditTrailDTO = new AuditTrailDTO(
                trim(auditTrail.flowCode()),
                trim(auditTrail.operationCode()),
                trim(auditTrail.description()),
                trim(auditTrail.defaultDescription()),
                trim(auditTrail.value()),
                trim(auditTrail.defaultValue()),
                trim(auditTrail.exception()),
                trim(auditTrail.defaultException()),
                auditTrail.mode());

        auditTrailDTO.setDataIn(createDataParamsDTO(auditTrail.dataIn()));
        auditTrailDTO.setDataOut(createDataParamsDTO(auditTrail.dataOut()));
        auditTrailDTO.setDataInOut(createDataParamsDTO(auditTrail.dataInOut()));

        return auditTrailDTO;
    }
}