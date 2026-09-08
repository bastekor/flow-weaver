package mx.bastekor.flowweaver.factory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.context.AuditTrailContainer;
import mx.bastekor.flowweaver.context.BusinessLogContainer;

import java.util.UUID;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestFactory {

    // ========== DataParamDTO ==========

    public static DataParamDTO aDataParamDTO() {
        return new DataParamDTO("k", "v", "d");
    }

    public static DataParamDTO aDataParamDTO(String key, String value, String defaultValue) {
        return new DataParamDTO(key, value, defaultValue);
    }

    public static DataParamDTO aDataParamDTO(Consumer<DataParamDTO> c) {
        DataParamDTO dto = aDataParamDTO();
        c.accept(dto);
        return dto;
    }

    // ========== DataParamsDTO ==========

    public static DataParamsDTO aDataParamsDTO() {
        return new DataParamsDTO();
    }

    public static DataParamsDTO aDataParamsDTO(DataParamDTO[] dataIn, DataParamDTO[] dataOut, DataParamDTO[] dataInOut) {
        DataParamsDTO dto = new DataParamsDTO();
        dto.setDataIn(dataIn);
        dto.setDataOut(dataOut);
        dto.setDataInOut(dataInOut);
        return dto;
    }

    // ========== BusinessLogDTO ==========

    public static BusinessLogDTO aBusinessLogDTO() {
        BusinessLogDTO dto = new BusinessLogDTO();
        dto.setGroup("G");
        dto.setCode("C");
        dto.setDescription("desc");
        dto.setDefaultDescription("defaultDesc");
        dto.setValue("val");
        dto.setDefaultValue("defaultVal");
        dto.setException("ex");
        dto.setDefaultException("defaultEx");
        dto.setMode(Mode.STATIC);
        return dto;
    }

    public static BusinessLogDTO aBusinessLogDTO(String group, String code, Mode mode) {
        BusinessLogDTO dto = aBusinessLogDTO();
        dto.setGroup(group);
        dto.setCode(code);
        dto.setMode(mode);
        return dto;
    }

    public static BusinessLogDTO aBusinessLogDTO(Consumer<BusinessLogDTO> c) {
        BusinessLogDTO dto = aBusinessLogDTO();
        c.accept(dto);
        return dto;
    }

    // ========== AuditTrailDTO ==========

    public static AuditTrailDTO anAuditTrailDTO() {
        AuditTrailDTO dto = new AuditTrailDTO();
        dto.setFlowCode("BL");
        dto.setGroup("G");
        dto.setCode("AT");
        dto.setDescription("desc");
        dto.setDefaultDescription("defaultDesc");
        dto.setValue("val");
        dto.setDefaultValue("defaultVal");
        dto.setException("ex");
        dto.setDefaultException("defaultEx");
        dto.setMode(Mode.STATIC);
        return dto;
    }

    public static AuditTrailDTO anAuditTrailDTO(String flowCode, String group, String code, Mode mode) {
        AuditTrailDTO dto = anAuditTrailDTO();
        dto.setFlowCode(flowCode);
        dto.setGroup(group);
        dto.setCode(code);
        dto.setMode(mode);
        return dto;
    }

    public static AuditTrailDTO anAuditTrailDTO(Consumer<AuditTrailDTO> c) {
        AuditTrailDTO dto = anAuditTrailDTO();
        c.accept(dto);
        return dto;
    }

    // ========== RequestDTO ==========

    public static RequestDTO aRequestDTO() {
        RequestDTO dto = new RequestDTO();
        dto.setId(UUID.randomUUID().toString());
        dto.setGroup("G");
        dto.setCode("C");
        dto.setDescription("desc");
        dto.setStatus(StatusEnum.SOURCE_SUCCESS.name());
        dto.setResult("result");
        dto.setMode("STATIC");
        return dto;
    }

    public static RequestDTO aRequestDTO(String id, String group, String code) {
        RequestDTO dto = aRequestDTO();
        dto.setId(id);
        dto.setGroup(group);
        dto.setCode(code);
        return dto;
    }

    public static RequestDTO aRequestDTO(Consumer<RequestDTO> c) {
        RequestDTO dto = aRequestDTO();
        c.accept(dto);
        return dto;
    }

    // ========== BusinessLogContainer ==========

    public static BusinessLogContainer aBusinessLogContainer(BusinessLog businessLog) {
        return aBusinessLogContainer(UUID.randomUUID().toString(), "GROUP", "CODE", businessLog);
    }

    public static BusinessLogContainer aBusinessLogContainer(String correlationId, String group, String code,
                                                              BusinessLog businessLog) {
        BusinessLogContainer c = new BusinessLogContainer(correlationId, group, code);
        c.setBusinessLog(businessLog);
        c.setStatus(StatusEnum.SOURCE_SUCCESS);
        c.setExitSignature("exit-signature");
        c.setResponse("response");
        return c;
    }

    public static BusinessLogContainer aBusinessLogContainer(String correlationId, String group, String code,
                                                              BusinessLog businessLog, Consumer<BusinessLogContainer> customizer) {
        BusinessLogContainer c = aBusinessLogContainer(correlationId, group, code, businessLog);
        customizer.accept(c);
        return c;
    }

    // ========== AuditTrailContainer ==========

    public static AuditTrailContainer anAuditTrailContainer(AuditTrail auditTrail) {
        return anAuditTrailContainer("GROUP", "PARENT", "CODE", auditTrail);
    }

    public static AuditTrailContainer anAuditTrailContainer(String group, String parentCode, String code,
                                                             AuditTrail auditTrail) {
        AuditTrailContainer c = new AuditTrailContainer();
        c.setGroup(group);
        c.setParentCode(parentCode);
        c.setCode(code);
        c.setAuditTrail(auditTrail);
        return c;
    }

    public static AuditTrailContainer anAuditTrailContainer(String group, String parentCode, String code,
                                                             AuditTrail auditTrail, Consumer<AuditTrailContainer> customizer) {
        AuditTrailContainer c = anAuditTrailContainer(group, parentCode, code, auditTrail);
        customizer.accept(c);
        return c;
    }
}
