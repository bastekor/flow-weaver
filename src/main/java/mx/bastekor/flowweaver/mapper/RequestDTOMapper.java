package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.config.FlowWeaverRootConfig;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.util.ResolveHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@Mapper(componentModel = "spring", imports = {ResolveHelper.class, StatusEnum.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RequestDTOMapper {

    @Autowired
    private FlowWeaverRootConfig config;

    @Autowired
    private UtilMapper utilMapper;

    // ============================================================
    //  MapStruct mapping: BusinessLogDTO → RequestDTO
    // ============================================================

    @Mapping(target = "id", source = "dto.correlationId")
    @Mapping(target = "group", source = "dto.group")
    @Mapping(target = "code", source = "dto.code")
    @Mapping(target = "description", expression = "java(ResolveHelper.resolve(dto.getDescription(), dto.getDefaultDescription(), jsonReq, jsonRes))")
    @Mapping(target = "result", expression = "java(ResolveHelper.resolveResult(jsonReq, jsonRes, dto))")
    @Mapping(target = "status", expression = "java(status.name())")
    @Mapping(target = "mode", expression = "java(dto.getMode().name())")
    @Mapping(target = "data", expression = "java(ResolveHelper.buildData(jsonReq, jsonRes, dto))")
    protected abstract RequestDTO map(BusinessLogDTO dto, String jsonReq, String jsonRes, StatusEnum status);

    // ============================================================
    //  Public entry points
    // ============================================================

    public RequestDTO build(final BusinessLogContainer container, final Environment env) {
        BusinessLogDTO dto = resolveBusinessLog(container);
        if (dto == null) return null;
        dto.setCorrelationId(container.getCorrelationId());
        RequestDTO requestDTO = map(dto, container.getExitSignature(), container.getResponse(), container.getStatus());
        fillInfrastructure(requestDTO, env);
        return requestDTO;
    }

    public RequestDTO build(final AuditTrailContainer container, final Environment env) {
        AuditTrailDTO dto = resolveAuditTrail(container);
        if (dto == null) return null;
        dto.setCorrelationId(container.getCorrelationId());
        String jsonReq = defaultIfBlank(container.getEntrySignature(), container.getExitSignature());
        RequestDTO requestDTO = map(dto, jsonReq, container.getResponse(), container.getStatus());
        fillInfrastructure(requestDTO, env);
        return requestDTO;
    }

    // ============================================================
    //  Resolution by mode (STATIC / DYNAMIC / MERGED)
    // ============================================================

    private BusinessLogDTO resolveBusinessLog(final BusinessLogContainer container) {
        return switch (container.getBusinessLog().mode()) {
            case STATIC -> BusinessLogMapper.createBusinessLogDTO(container);
            case DYNAMIC -> ofNullable(config.getBusinessLogs())
                    .map(bl -> bl.get(container.getCode()))
                    .orElse(null);
            case MERGED -> {
                var dynamic = ofNullable(config.getBusinessLogs())
                        .map(bl -> bl.get(container.getCode()))
                        .orElse(null);
                var staticDto = BusinessLogMapper.createBusinessLogDTO(container);
                yield dynamic == null ? staticDto : utilMapper.mergeBusinessLogDTO(dynamic, staticDto);
            }
        };
    }

    private AuditTrailDTO resolveAuditTrail(final AuditTrailContainer container) {
        return switch (container.getAuditTrail().mode()) {
            case STATIC -> AuditTrailMapper.createAuditTrailDTO(container);
            case DYNAMIC -> ofNullable(config.getAuditTrails())
                    .map(at -> at.get(container.getCode()))
                    .orElse(null);
            case MERGED -> {
                var dynamic = ofNullable(config.getAuditTrails())
                        .map(at -> at.get(container.getCode()))
                        .orElse(null);
                var staticDto = AuditTrailMapper.createAuditTrailDTO(container);
                yield dynamic == null ? staticDto : utilMapper.mergeAuditTrailDTO(dynamic, staticDto);
            }
        };
    }

    // ============================================================
    //  Infrastructure enrichment
    // ============================================================

    private void fillInfrastructure(final RequestDTO requestDTO, final Environment env) {
        ResolveHelper.fillAppInfo(requestDTO, env);
        ResolveHelper.fillInfrastructureInfo(requestDTO, env);
        ResolveHelper.getHostNameAndIpAddress(requestDTO);
    }
}
