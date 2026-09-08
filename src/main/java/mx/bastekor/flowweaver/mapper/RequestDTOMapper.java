package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.config.FlowWeaverRootConfig;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.context.AuditTrailContainer;
import mx.bastekor.flowweaver.context.BusinessLogContainer;
import mx.bastekor.flowweaver.resolver.ResolutionResult;
import mx.bastekor.flowweaver.util.ResolveHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static mx.bastekor.flowweaver.enums.Phase.ENTRY;
import static mx.bastekor.flowweaver.enums.Phase.EXIT;
import static mx.bastekor.flowweaver.mapper.AuditTrailMapper.createAuditTrailDTO;
import static mx.bastekor.flowweaver.mapper.BusinessLogMapper.createBusinessLogDTO;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@Mapper(componentModel = "spring", imports = {ResolveHelper.class, StatusEnum.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RequestDTOMapper {

    @Autowired
    private FlowWeaverRootConfig config;

    @Autowired
    private UtilMapper utilMapper;

    @Autowired
    private Environment environment;

    @Mapping(target = "id", source = "dto.correlationId")
    @Mapping(target = "group", source = "dto.group")
    @Mapping(target = "code", source = "dto.code")
    @Mapping(target = "description", expression = "java(ResolveHelper.resolveDescription(jsonReq, jsonRes, dto, resolutions))")
    @Mapping(target = "result", expression = "java(ResolveHelper.resolveResult(jsonReq, jsonRes, dto, resolutions))")
    @Mapping(target = "status", expression = "java(status.name())")
    @Mapping(target = "mode", expression = "java(dto.getMode().name())")
    @Mapping(target = "data", expression = "java(ResolveHelper.resolveData(jsonReq, jsonRes, dto, resolutions))")
    @Mapping(target = "resolutions", ignore = true)
    @Mapping(target = "appName", ignore = true)
    @Mapping(target = "appVersion", ignore = true)
    @Mapping(target = "appDescription", ignore = true)
    @Mapping(target = "hostName", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "instanceId", ignore = true)
    @Mapping(target = "region", ignore = true)
    @Mapping(target = "zone", ignore = true)
    @Mapping(target = "phase", ignore = true)
    protected abstract RequestDTO map(BusinessLogDTO dto, String jsonReq, String jsonRes, StatusEnum status, Map<String, ResolutionResult> resolutions);

    public RequestDTO build(final BusinessLogContainer container, final BusinessLogDTO dto) {
        if (dto == null) return null;
        dto.setCorrelationId(container.getCorrelationId());
        Map<String, ResolutionResult> resolutions = new HashMap<>();
        RequestDTO requestDTO = map(dto, container.getExitSignature(), container.getResponse(), container.getStatus(), resolutions);
        requestDTO.setResolutions(resolutions);
        fillInfrastructure(requestDTO);
        requestDTO.setPhase(EXIT);
        return requestDTO;
    }

    public RequestDTO build(final AuditTrailContainer container, final AuditTrailDTO dto) {
        if (dto == null) return null;
        dto.setCorrelationId(container.getCorrelationId());
        String jsonReq = defaultIfBlank(container.getEntrySignature(), container.getExitSignature());
        Map<String, ResolutionResult> resolutions = new HashMap<>();
        RequestDTO requestDTO = map(dto, jsonReq, container.getResponse(), container.getStatus(), resolutions);
        requestDTO.setResolutions(resolutions);
        fillInfrastructure(requestDTO);
        requestDTO.setPhase(container.getResponse() == null ? ENTRY : EXIT);
        return requestDTO;
    }

    public BusinessLogDTO resolveBusinessLog(final BusinessLogContainer container) {
        return switch (container.getBusinessLog().mode()) {
            case STATIC -> createBusinessLogDTO(container);
            case DYNAMIC -> ofNullable(config.getBusinessLogs())
                    .map(bl -> bl.get(container.getCode()))
                    .orElse(null);
            case MERGED -> {
                var dynamic = ofNullable(config.getBusinessLogs())
                        .map(bl -> bl.get(container.getCode()))
                        .orElse(null);
                var staticDto = createBusinessLogDTO(container);
                yield dynamic == null ? staticDto : utilMapper.mergeBusinessLogDTO(dynamic, staticDto);
            }
        };
    }

    public AuditTrailDTO resolveAuditTrail(final AuditTrailContainer container) {
        return switch (container.getAuditTrail().mode()) {
            case STATIC -> createAuditTrailDTO(container);
            case DYNAMIC -> ofNullable(config.getAuditTrails())
                    .map(at -> at.get(container.getCode()))
                    .orElse(null);
            case MERGED -> {
                var dynamic = ofNullable(config.getAuditTrails())
                        .map(at -> at.get(container.getCode()))
                        .orElse(null);
                var staticDto = createAuditTrailDTO(container);
                yield dynamic == null ? staticDto : utilMapper.mergeAuditTrailDTO(dynamic, staticDto);
            }
        };
    }

    private void fillInfrastructure(final RequestDTO requestDTO) {
        ResolveHelper.fillAppInfo(requestDTO, environment);
        ResolveHelper.fillInfrastructureInfo(requestDTO, environment);
        ResolveHelper.getHostNameAndIpAddress(requestDTO);
    }
}