package mx.bastekor.flowweaver.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.model.BusinessLogContainer;

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
     * @param businessLogContainer Objeto de negocio en hilo.
     * @return {@link BusinessLogDTO} mapeado.
     */
    public static BusinessLogDTO createBusinessLogDTO(final BusinessLogContainer businessLogContainer) {

        if (businessLogContainer == null) {
            return null;
        }

        if (businessLogContainer.getBusinessLog() == null) {
            return null;
        }

        BusinessLogDTO businessLogDTO = new BusinessLogDTO();
        // Se agrega groupCode y operationCode en caso de que hayan sido vacíos desde @BusinessLog
        businessLogDTO.setGroupCode(businessLogContainer.getGroupCode());
        businessLogDTO.setOperationCode(businessLogContainer.getOperationCode());

        businessLogDTO.setDescription(trim(businessLogContainer.getBusinessLog().description()));
        businessLogDTO.setDefaultDescription(trim(businessLogContainer.getBusinessLog().defaultDescription()));
        businessLogDTO.setValue(trim(businessLogContainer.getBusinessLog().value()));
        businessLogDTO.setDefaultValue(trim(businessLogContainer.getBusinessLog().defaultValue()));
        businessLogDTO.setException(trim(businessLogContainer.getBusinessLog().exception()));
        businessLogDTO.setDefaultException(trim(businessLogContainer.getBusinessLog().defaultException()));
        businessLogDTO.setMode(businessLogContainer.getBusinessLog().mode());
        businessLogDTO.setDataOut(createDataParamsDTO(businessLogContainer.getBusinessLog().dataOut()));
        return businessLogDTO;
    }
}