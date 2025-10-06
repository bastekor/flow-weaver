package mx.bastekor.flowweaver.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;

/**
 * Clase Mapeadora de {@link BusinessLog} a {@link BusinessLogDTO}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessLogMapper {

    /**
     * Método encargado de mapear de {@link BusinessLog} a {@link BusinessLogDTO}.
     * @param businessLog Anotación de negocio.
     * @return {@link BusinessLogDTO} mapeado.
     */
    public static BusinessLogDTO createBusinessLogDTO(BusinessLog businessLog) {
        if (businessLog == null) {
            return null;
        }

        BusinessLogDTO businessLogDTO = new BusinessLogDTO();
        businessLogDTO.setFlowCode(businessLog.operationCode());
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

    /**
     * Método encargado de crear un array de {@link DataParamDTO} a partir de un array de {@link DataParam}.
     * @param dataParams Array de {@link DataParam}.
     * @return Array de {@link DataParamDTO}.
     */
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

    /**
     * Método encargado de crear un {@link DataParamDTO} a partir de un {@link DataParam}.
     * @param dataParam {@link DataParam}.
     * @return {@link DataParamDTO}.
     */
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