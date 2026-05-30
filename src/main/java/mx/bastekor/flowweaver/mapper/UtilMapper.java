package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.helper.MergeHelper;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

@Mapper(
        componentModel = "spring",
        imports = {MergeHelper.class},
        nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_NULL
)
public interface UtilMapper {

    @Mapping(target = "key", expression = "java(MergeHelper.resolve(priority.getKey(), fallback.getKey()))")
    @Mapping(target = "value", expression = "java(MergeHelper.resolve(priority.getValue(), fallback.getValue()))")
    @Mapping(target = "defaultValue", expression = "java(MergeHelper.resolve(priority.getDefaultValue(), fallback.getDefaultValue()))")
    DataParamDTO mergeDataParamDTO(DataParamDTO priority, DataParamDTO fallback);

    default DataParamDTO[] mergeDataParamDTOArrays(DataParamDTO[] priority, DataParamDTO[] fallback) {
        java.util.Map<String, DataParamDTO> fallbackIndex = new java.util.HashMap<>();
        if (fallback != null) {
            for (DataParamDTO f : fallback) {
                if (f != null && f.getKey() != null && !f.getKey().isBlank()) {
                    fallbackIndex.put(f.getKey(), f);
                }
            }
        }

        java.util.List<DataParamDTO> result = new java.util.ArrayList<>();
        java.util.Set<String> matchedKeys = new java.util.HashSet<>();

        if (priority != null) {
            for (DataParamDTO p : priority) {
                if (p == null || p.getKey() == null || p.getKey().isBlank()) continue;
                DataParamDTO f = fallbackIndex.get(p.getKey());
                if (f != null) {
                    result.add(mergeDataParamDTO(p, f));
                    matchedKeys.add(p.getKey());
                } else {
                    result.add(p);
                }
            }
        }

        for (java.util.Map.Entry<String, DataParamDTO> entry : fallbackIndex.entrySet()) {
            if (!matchedKeys.contains(entry.getKey())) {
                result.add(entry.getValue());
            }
        }

        return result.isEmpty() ? null : result.toArray(new DataParamDTO[0]);
    }

    @Mapping(target = "dataIn", expression = "java(mergeDataParamDTOArrays(priority.getDataIn(), fallback.getDataIn()))")
    @Mapping(target = "dataOut", expression = "java(mergeDataParamDTOArrays(priority.getDataOut(), fallback.getDataOut()))")
    @Mapping(target = "dataInOut", expression = "java(mergeDataParamDTOArrays(priority.getDataInOut(), fallback.getDataInOut()))")
    DataParamsDTO mergeDataParamsDTO(DataParamsDTO priority, DataParamsDTO fallback);

    @InheritConfiguration(name = "mergeDataParamsDTO")
    @Mapping(target = "correlationId", ignore = true)
    @Mapping(target = "group", expression = "java(MergeHelper.resolve(priority.getGroup(), fallback.getGroup()))")
    @Mapping(target = "code", expression = "java(MergeHelper.resolve(priority.getCode(), fallback.getCode()))")
    @Mapping(target = "description", expression = "java(MergeHelper.resolve(priority.getDescription(), fallback.getDescription()))")
    @Mapping(target = "defaultDescription", expression = "java(MergeHelper.resolve(priority.getDefaultDescription(), fallback.getDefaultDescription()))")
    @Mapping(target = "value", expression = "java(MergeHelper.resolve(priority.getValue(), fallback.getValue()))")
    @Mapping(target = "defaultValue", expression = "java(MergeHelper.resolve(priority.getDefaultValue(), fallback.getDefaultValue()))")
    @Mapping(target = "exception", expression = "java(MergeHelper.resolve(priority.getException(), fallback.getException()))")
    @Mapping(target = "defaultException", expression = "java(MergeHelper.resolve(priority.getDefaultException(), fallback.getDefaultException()))")
    @Mapping(target = "mode", expression = "java(MergeHelper.resolve(priority.getMode(), fallback.getMode()))")
    BusinessLogDTO mergeBusinessLogDTO(BusinessLogDTO priority, BusinessLogDTO fallback);

    @InheritConfiguration(name = "mergeBusinessLogDTO")
    @Mapping(target = "flowCode", expression = "java(MergeHelper.resolve(priority.getFlowCode(), fallback.getFlowCode()))")
    AuditTrailDTO mergeAuditTrailDTO(AuditTrailDTO priority, AuditTrailDTO fallback);
}