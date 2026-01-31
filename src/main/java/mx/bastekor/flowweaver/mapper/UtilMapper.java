package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
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

    @Mapping(target = "dataIn", expression = "java(MergeHelper.resolve(priority.getDataIn(), fallback.getDataIn()))")
    @Mapping(target = "dataOut", expression = "java(MergeHelper.resolve(priority.getDataOut(), fallback.getDataOut()))")
    @Mapping(target = "dataInOut", expression = "java(MergeHelper.resolve(priority.getDataInOut(), fallback.getDataInOut()))")
    DataParamsDTO mergeDataParamsDTO(DataParamsDTO priority, DataParamsDTO fallback);

    @InheritConfiguration(name = "mergeDataParamsDTO")
    @Mapping(target = "groupCode", expression = "java(MergeHelper.resolve(priority.getGroupCode(), fallback.getGroupCode()))")
    @Mapping(target = "operationCode", expression = "java(MergeHelper.resolve(priority.getOperationCode(), fallback.getOperationCode()))")
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