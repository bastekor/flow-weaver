package mx.bastekor.flowweaver.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.dto.DataParamDTO;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataParamMapper {

    /**
     * Método encargado de crear un array de {@link DataParamDTO} a partir de un array de {@link DataParam}.
     *
     * @param dataParams Array de {@link DataParam}.
     * @return Array de {@link DataParamDTO}.
     */
    public static DataParamDTO[] createDataParamsDTO(DataParam[] dataParams) {
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
     *
     * @param dataParam {@link DataParam}.
     * @return {@link DataParamDTO}.
     */
    public static DataParamDTO createDataParamDTO(DataParam dataParam) {
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