package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.model.BusinessLogDTO;
import mx.bastekor.flowweaver.model.DataParamDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BusinessLogMapper {
    BusinessLogMapper INSTANCE = Mappers.getMapper(BusinessLogMapper.class);

    @Mapping(target = "operationCode", expression = "java(businessLog.operationCode())")
    @Mapping(target = "description", expression = "java(businessLog.description())")
    @Mapping(target = "defaultDescription", expression = "java(businessLog.defaultDescription())")
    @Mapping(target = "value", expression = "java(businessLog.value())")
    @Mapping(target = "defaultValue", expression = "java(businessLog.defaultValue())")
    @Mapping(target = "exception", expression = "java(businessLog.exception())")
    @Mapping(target = "defaultException", expression = "java(businessLog.defaultException())")
    @Mapping(target = "mode", expression = "java(businessLog.mode())")
    @Mapping(target = "dataOut", expression = "java(createDataParamsDTO(businessLog.dataOut()))")
    BusinessLogDTO createBusinessLogDTO(BusinessLog businessLog);

    default DataParamDTO[] createDataParamsDTO(DataParam[] dataParams) {
        if (dataParams == null) {
            return null;
        }

        DataParamDTO[] dataParamDTOs = new DataParamDTO[dataParams.length];
        for (int i = 0; i < dataParams.length; i++) {
            if (dataParams[i] != null) {
                dataParamDTOs[i] = createDataParamDTO(dataParams[i]);
            }
        }
        return dataParamDTOs;
    }

    @Mapping(target = "key", expression = "java(dataParam.key())")
    @Mapping(target = "value", expression = "java(dataParam.value())")
    @Mapping(target = "defaultValue", expression = "java(dataParam.defaultValue())")
    DataParamDTO createDataParamDTO(DataParam dataParam);
}