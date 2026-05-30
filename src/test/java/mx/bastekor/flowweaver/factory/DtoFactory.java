package mx.bastekor.flowweaver.factory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.enums.StatusEnum;

import java.util.UUID;
import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DtoFactory {

    public static DataParamDTO createDataParamDTO() {
        return createDataParamDTO("k", "v", "d");
    }

    public static DataParamDTO createDataParamDTO(String key, String value, String defaultValue) {
        return new DataParamDTO(key, value, defaultValue);
    }

    public static DataParamsDTO createDataParamsDTO() {
        return new DataParamsDTO();
    }

    public static DataParamsDTO createDataParamsDTO(DataParamDTO[] dataIn, DataParamDTO[] dataOut, DataParamDTO[] dataInOut) {
        DataParamsDTO dto = new DataParamsDTO();
        dto.setDataIn(dataIn);
        dto.setDataOut(dataOut);
        dto.setDataInOut(dataInOut);
        return dto;
    }

    public static BusinessLogDTO createBusinessLogDTO() {
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

    public static BusinessLogDTO createBusinessLogDTO(String group, String code, Mode mode) {
        BusinessLogDTO dto = createBusinessLogDTO();
        dto.setGroup(group);
        dto.setCode(code);
        dto.setMode(mode);
        return dto;
    }

    public static BusinessLogDTO createBusinessLogDTO(Consumer<BusinessLogDTO> c) {
        BusinessLogDTO dto = createBusinessLogDTO();
        c.accept(dto);
        return dto;
    }

    public static AuditTrailDTO createAuditTrailDTO() {
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

    public static AuditTrailDTO createAuditTrailDTO(String flowCode, String group, String code, Mode mode) {
        AuditTrailDTO dto = createAuditTrailDTO();
        dto.setFlowCode(flowCode);
        dto.setGroup(group);
        dto.setCode(code);
        dto.setMode(mode);
        return dto;
    }

    public static AuditTrailDTO createAuditTrailDTO(Consumer<AuditTrailDTO> c) {
        AuditTrailDTO dto = createAuditTrailDTO();
        c.accept(dto);
        return dto;
    }

    public static RequestDTO createRequestDTO() {
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

    public static RequestDTO createRequestDTO(String id, String group, String code) {
        RequestDTO dto = createRequestDTO();
        dto.setId(id);
        dto.setGroup(group);
        dto.setCode(code);
        return dto;
    }

    public static RequestDTO createRequestDTO(Consumer<RequestDTO> c) {
        RequestDTO dto = createRequestDTO();
        c.accept(dto);
        return dto;
    }
}
