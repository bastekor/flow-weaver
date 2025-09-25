package mx.bastekor.flowweaver.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.model.BusinessLogDTO;
import mx.bastekor.flowweaver.model.DataParamDTO;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessLogMapper {

    public static BusinessLogDTO createBusinessLogDTO(BusinessLog businessLog) {
        if (businessLog == null) {
            return null;
        }

        BusinessLogDTO businessLogDTO = new BusinessLogDTO();
        businessLogDTO.setOperationCode(businessLog.operationCode());
        businessLogDTO.setDescription(businessLog.description());
        businessLogDTO.setDefaultDescription(businessLog.defaultDescription());
        businessLogDTO.setValue(businessLog.value());
        businessLogDTO.setDefaultValue(businessLog.defaultValue());
        businessLogDTO.setException(businessLog.exception());
        businessLogDTO.setDefaultException(businessLog.defaultException());
        businessLogDTO.setMode(businessLog.mode());
        businessLogDTO.setDataOut(createDataParamsDTO(businessLog.dataOut()));

        return businessLogDTO;
    }

    private static DataParamDTO[] createDataParamsDTO(DataParam[] dataParams) {
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

    private static DataParamDTO createDataParamDTO(DataParam dataParam) {
        if (dataParam == null) {
            return null;
        }

        DataParamDTO dataParamDTO = new DataParamDTO();

        dataParamDTO.setKey(dataParam.key());
        dataParamDTO.setValue(dataParam.value());
        dataParamDTO.setDefaultValue(dataParam.defaultValue());

        return dataParamDTO;
    }
}