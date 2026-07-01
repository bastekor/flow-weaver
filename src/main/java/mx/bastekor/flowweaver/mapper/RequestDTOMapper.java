package mx.bastekor.flowweaver.mapper;

import mx.bastekor.flowweaver.config.FlowWeaverRootConfig;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.dto.SimpleRequestDTO;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.resolver.ResolutionResult;
import mx.bastekor.flowweaver.util.ResolveHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static mx.bastekor.flowweaver.enums.Phase.ENTRY;
import static mx.bastekor.flowweaver.enums.Phase.EXIT;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@Mapper(componentModel = "spring", imports = {ResolveHelper.class, StatusEnum.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RequestDTOMapper {

    @Autowired
    private FlowWeaverRootConfig config;

    @Autowired
    private UtilMapper utilMapper;

    @Mapping(target = "id", source = "dto.correlationId")
    @Mapping(target = "group", source = "dto.group")
    @Mapping(target = "code", source = "dto.code")
    @Mapping(target = "status", expression = "java(status.name())")
    @Mapping(target = "mode", expression = "java(dto.getMode().name())")
    protected abstract RequestDTO mapBase(BusinessLogDTO dto, StatusEnum status);

    public RequestDTO build(final BusinessLogContainer container, final Environment env) {
        BusinessLogDTO dto = resolveBusinessLog(container);
        if (dto == null) return null;
        dto.setCorrelationId(container.getCorrelationId());

        String jsonReq = container.getExitSignature();
        String jsonRes = container.getResponse();

        RequestDTO requestDTO = mapBase(dto, container.getStatus());

        Map<String, ResolutionResult> resolution = new LinkedHashMap<>();

        ResolutionResult desc = ResolveHelper.resolve(
                dto.getDescription(), dto.getDefaultDescription(), jsonReq, jsonRes);
        requestDTO.setDescription(desc.getValue());
        resolution.put("description", desc);

        ResolutionResult result = ResolveHelper.resolveResult(jsonReq, jsonRes, dto);
        requestDTO.setResult(result.getValue());
        resolution.put("result", result);

        requestDTO.setData(ResolveHelper.buildDataWithDiagnostics(
                jsonReq, jsonRes, dto, resolution));

        requestDTO.setResolution(resolution);
        fillInfrastructure(requestDTO, env);
        requestDTO.setPhase(EXIT);

        return requestDTO;
    }

    public RequestDTO build(final AuditTrailContainer container, final Environment env) {
        AuditTrailDTO dto = resolveAuditTrail(container);
        if (dto == null) return null;
        dto.setCorrelationId(container.getCorrelationId());

        String jsonReq = defaultIfBlank(container.getEntrySignature(), container.getExitSignature());
        String jsonRes = container.getResponse();

        RequestDTO requestDTO = mapBase(dto, container.getStatus());

        Map<String, ResolutionResult> resolution = new LinkedHashMap<>();

        ResolutionResult desc = ResolveHelper.resolve(
                dto.getDescription(), dto.getDefaultDescription(), jsonReq, jsonRes);
        requestDTO.setDescription(desc.getValue());
        resolution.put("description", desc);

        ResolutionResult result = ResolveHelper.resolveResult(jsonReq, jsonRes, dto);
        requestDTO.setResult(result.getValue());
        resolution.put("result", result);

        requestDTO.setData(ResolveHelper.buildDataWithDiagnostics(
                jsonReq, jsonRes, dto, resolution));

        requestDTO.setResolution(resolution);
        fillInfrastructure(requestDTO, env);
        requestDTO.setPhase(jsonRes == null ? ENTRY : EXIT);

        return requestDTO;
    }

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

    private void fillInfrastructure(final SimpleRequestDTO requestDTO, final Environment env) {
        ResolveHelper.fillAppInfo(requestDTO, env);
        ResolveHelper.fillInfrastructureInfo(requestDTO, env);
        ResolveHelper.getHostNameAndIpAddress(requestDTO);
    }
}
