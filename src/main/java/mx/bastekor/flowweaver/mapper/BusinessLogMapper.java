package mx.bastekor.flowweaver.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;

import static mx.bastekor.flowweaver.mapper.DataParamMapper.createDataParamsDTO;
import static org.apache.commons.lang3.StringUtils.trim;

/**
 * Clase Mapeadora de {@link BusinessLog} a {@link BusinessLogDTO}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessLogMapper {

    /**
     * Método encargado de mapear de {@link BusinessLog} a {@link BusinessLogDTO}.
     *
     * @param businessLog Anotación de negocio.
     * @return {@link BusinessLogDTO} mapeado.
     */
    public static BusinessLogDTO createBusinessLogDTO(BusinessLog businessLog) {

        if (businessLog == null) {
            return null;
        }

        BusinessLogDTO businessLogDTO = new BusinessLogDTO();
        businessLogDTO.setOperationCode(trim(businessLog.operationCode()));
        businessLogDTO.setDescription(trim(businessLog.description()));
        businessLogDTO.setDefaultDescription(trim(businessLog.defaultDescription()));
        businessLogDTO.setValue(trim(businessLog.value()));
        businessLogDTO.setDefaultValue(trim(businessLog.defaultValue()));
        businessLogDTO.setException(trim(businessLog.exception()));
        businessLogDTO.setDefaultException(trim(businessLog.defaultException()));
        businessLogDTO.setMode(businessLog.mode());
        businessLogDTO.setDataOut(createDataParamsDTO(businessLog.dataOut()));
        return businessLogDTO;
    }
}