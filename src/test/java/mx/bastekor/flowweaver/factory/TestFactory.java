package mx.bastekor.flowweaver.factory;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;

import java.util.UUID;
import java.util.function.Consumer;

public class TestFactory {

    private final BusinessLog businessLog;
    private final AuditTrail auditTrail;

    private TestFactory(BusinessLog businessLog, AuditTrail auditTrail) {
        this.businessLog = businessLog;
        this.auditTrail = auditTrail;
    }

    public static TestFactory create(BusinessLog businessLog, AuditTrail auditTrail) {
        return new TestFactory(businessLog, auditTrail);
    }

    public static TestFactory dtoOnly() {
        return new TestFactory(null, null);
    }

    // ========== DataParamDTO ==========

    @SuppressWarnings("unchecked")
    public DataParamDTO aDataParamDTO(Consumer<DataParamDTO>... customizers) {
        DataParamDTO dto = new DataParamDTO("k", "v", "d");
        for (var c : customizers) c.accept(dto);
        return dto;
    }

    public DataParamDTO aDataParamDTO(String key, String value, String defaultValue) {
        return new DataParamDTO(key, value, defaultValue);
    }

    // ========== DataParamsDTO ==========

    @SuppressWarnings("unchecked")
    public DataParamsDTO aDataParamsDTO(Consumer<DataParamsDTO>... customizers) {
        DataParamsDTO dto = new DataParamsDTO();
        for (var c : customizers) c.accept(dto);
        return dto;
    }

    public DataParamsDTO aDataParamsDTO(DataParamDTO[] dataIn, DataParamDTO[] dataOut, DataParamDTO[] dataInOut) {
        DataParamsDTO dto = new DataParamsDTO();
        dto.setDataIn(dataIn);
        dto.setDataOut(dataOut);
        dto.setDataInOut(dataInOut);
        return dto;
    }

    // ========== BusinessLogDTO ==========

    @SuppressWarnings("unchecked")
    public BusinessLogDTO aBusinessLogDTO(Consumer<BusinessLogDTO>... customizers) {
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
        for (var c : customizers) c.accept(dto);
        return dto;
    }

    public BusinessLogDTO aBusinessLogDTO(String group, String code, Mode mode) {
        BusinessLogDTO dto = aBusinessLogDTO();
        dto.setGroup(group);
        dto.setCode(code);
        dto.setMode(mode);
        return dto;
    }

    // ========== AuditTrailDTO ==========

    @SuppressWarnings("unchecked")
    public AuditTrailDTO anAuditTrailDTO(Consumer<AuditTrailDTO>... customizers) {
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
        for (var c : customizers) c.accept(dto);
        return dto;
    }

    public AuditTrailDTO anAuditTrailDTO(String flowCode, String group, String code, Mode mode) {
        AuditTrailDTO dto = anAuditTrailDTO();
        dto.setFlowCode(flowCode);
        dto.setGroup(group);
        dto.setCode(code);
        dto.setMode(mode);
        return dto;
    }

    // ========== RequestDTO ==========

    @SuppressWarnings("unchecked")
    public RequestDTO aRequestDTO(Consumer<RequestDTO>... customizers) {
        RequestDTO dto = new RequestDTO();
        dto.setId(UUID.randomUUID().toString());
        dto.setGroup("G");
        dto.setCode("C");
        dto.setDescription("desc");
        dto.setStatus(StatusEnum.SOURCE_SUCCESS.name());
        dto.setResult("result");
        dto.setMode("STATIC");
        for (var c : customizers) c.accept(dto);
        return dto;
    }

    // ========== BusinessLogContainer ==========

    public BusinessLogContainer aBusinessLogContainer() {
        return aBusinessLogContainer(UUID.randomUUID().toString(), "GROUP", "CODE");
    }

    @SuppressWarnings("unchecked")
    public BusinessLogContainer aBusinessLogContainer(String correlationId, String group, String code,
                                                       Consumer<BusinessLogContainer>... customizers) {
        BusinessLogContainer c = new BusinessLogContainer(correlationId, group, code);
        c.setBusinessLog(businessLog);
        c.setStatus(StatusEnum.SOURCE_SUCCESS);
        c.setExitSignature("exit-signature");
        c.setResponse("response");
        for (var cust : customizers) cust.accept(c);
        return c;
    }

    // ========== AuditTrailContainer ==========

    @SuppressWarnings("unchecked")
    public AuditTrailContainer anAuditTrailContainer(Consumer<AuditTrailContainer>... customizers) {
        AuditTrailContainer c = new AuditTrailContainer();
        c.setGroup("GROUP");
        c.setParentCode("PARENT");
        c.setCode("CODE");
        c.setAuditTrail(auditTrail);
        for (var cust : customizers) cust.accept(c);
        return c;
    }
}
