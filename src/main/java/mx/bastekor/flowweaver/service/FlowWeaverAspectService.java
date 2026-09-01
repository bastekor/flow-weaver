package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.FlowWeaverRootConfig;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.exception.FlowWeaverException;
import mx.bastekor.flowweaver.handler.FlowWeaverResultHandler;
import mx.bastekor.flowweaver.mapper.RequestDTOMapper;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.util.FrameExtractor;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static mx.bastekor.flowweaver.util.Util.getDuration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowWeaverAspectService implements IFlowWeaverAspectService {

    private final Environment environment;
    private final FrameExtractor frameExtractor;
    private final RequestDTOMapper requestDTOMapper;
    private final FlowWeaverRootConfig flowWeaverRootConfig;
    private final FlowWeaverResultHandler flowWeaverResultHandler;


    @Override
    @Async("flowWeaverExecutor")
    public void processBusinessLog(final BusinessLogContainer businessLogContainer) {
        // Cuanto duro el metodo interceptado
        final String methodDuration = businessLogContainer.getDuration();
        final Instant start = Instant.now();
        final RequestDTO requestDTO = requestDTOMapper.build(businessLogContainer, environment);
        this.sendHandler(start, methodDuration, requestDTO);
    }

    @Override
    @Async("flowWeaverExecutor")
    public void processAuditTrail(final AuditTrailContainer auditTrailContainer) {
        // Cuanto tiempo duro el metodo interceptado...
        final String methodDuration = auditTrailContainer.getDuration();
        final Instant start = Instant.now();
        final RequestDTO requestDTO = requestDTOMapper.build(auditTrailContainer, environment);
        this.sendHandler(start, methodDuration, requestDTO);
    }

    private void sendHandler(final Instant start, final String methodDuration, final RequestDTO requestDTO) {
        Map<String, Object> fields = new HashMap<>();
        try {
            fields = frameExtractor.extract(requestDTO, flowWeaverRootConfig.getMaxDepth());
            fields.put("methodDuration", methodDuration);
        } catch (FlowWeaverException e) {
            fields.put("flowWeaverException", e);
            log.error("Error :: {}", e.getMessage(), e);
        } finally {
            final Instant end = Instant.now();
            // Cuanto tiempo tomo el mapeo de datos
            final String mappedDuration = getDuration(start, end);
            fields.put("mappedDuration", mappedDuration);
        }
        flowWeaverResultHandler.handle(requestDTO, fields);
    }
}