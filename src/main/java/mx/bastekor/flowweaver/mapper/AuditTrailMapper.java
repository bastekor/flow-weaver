package mx.bastekor.flowweaver.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.model.AuditTrailContainer;

import static mx.bastekor.flowweaver.mapper.DataParamMapper.createDataParamsDTO;
import static org.apache.commons.lang3.StringUtils.trim;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditTrailMapper {

    public static AuditTrailDTO createAuditTrailDTO(AuditTrailContainer auditTrailContainer) {

        if (auditTrailContainer == null || auditTrailContainer.getAuditTrail() == null) {
            return null;
        }

        final AuditTrailDTO auditTrailDTO = new AuditTrailDTO();
        auditTrailDTO.setGroup(auditTrailContainer.getGroup());
        auditTrailDTO.setFlowCode(auditTrailContainer.getParentCode());
        auditTrailDTO.setCode(auditTrailContainer.getCode());
        auditTrailDTO.setDescription(trim(auditTrailContainer.getAuditTrail().description()));
        auditTrailDTO.setDefaultDescription(trim(auditTrailContainer.getAuditTrail().defaultDescription()));
        auditTrailDTO.setValue(trim(auditTrailContainer.getAuditTrail().value()));
        auditTrailDTO.setDefaultValue(trim(auditTrailContainer.getAuditTrail().defaultValue()));
        auditTrailDTO.setException(trim(auditTrailContainer.getAuditTrail().exception()));
        auditTrailDTO.setDefaultException(trim(auditTrailContainer.getAuditTrail().defaultException()));
        auditTrailDTO.setMode(auditTrailContainer.getAuditTrail().mode());
        auditTrailDTO.setDataIn(createDataParamsDTO(auditTrailContainer.getAuditTrail().dataIn()));
        auditTrailDTO.setDataOut(createDataParamsDTO(auditTrailContainer.getAuditTrail().dataOut()));
        auditTrailDTO.setDataInOut(createDataParamsDTO(auditTrailContainer.getAuditTrail().dataInOut()));

        return auditTrailDTO;
    }
}