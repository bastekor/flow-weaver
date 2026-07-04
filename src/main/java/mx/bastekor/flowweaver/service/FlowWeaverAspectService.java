package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.mapper.RequestDTOMapper;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.util.Util;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_ERROR;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_ERROR;
import static mx.bastekor.flowweaver.util.Util.getDuration;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowWeaverAspectService implements IFlowWeaverAspectService {

    private final Environment environment;
    private final RequestDTOMapper requestDTOMapper;
    private final IFlowWeaverService flowWeaverService;

    @Override
    @Async("flowWeaverExecutor")
    public void processBusinessLog(final BusinessLogContainer businessLogContainer) {
        // Cuanto duro el metodo interceptado
        final String methodDuration = businessLogContainer.getDuration();
        final Instant start = Instant.now();
        try {
            final RequestDTO requestDTO = requestDTOMapper.build(businessLogContainer, environment);
            flowWeaverService.trace(requestDTO);
//            requestDTO.getResolutions().forEach((k,v) -> System.out.println(k + ": " + v));
        } catch (Exception e) {
            log.error(BUSINESS_ERROR, businessLogContainer.getStatus(), businessLogContainer.getCode(), e.getMessage(), e);
        } finally {
            final Instant end = Instant.now();
            // Cuanto tiempo tomo el mapeo de datos
            final String mappedDuration = getDuration(start, end);
        }
    }

    @Override
    @Async("flowWeaverExecutor")
    public void processAuditTrail(final AuditTrailContainer auditTrailContainer) {
        // Cuanto tiempo duro el metodo interceptado...
        final String methodDuration = auditTrailContainer.getDuration();

        final Instant start = Instant.now();
        try {
            final RequestDTO requestDTO = requestDTOMapper.build(auditTrailContainer, environment);
            flowWeaverService.trace(requestDTO);
        } catch (Exception e) {
            log.error(AUDIT_ERROR, auditTrailContainer.getStatus(), auditTrailContainer.getCorrelationId(), e.getMessage(), e);
        } finally {
            final Instant end = Instant.now();
            // Cuanto tiempo tomo el mapeo de datos
            final String mappedDuration = getDuration(start, end);
        }
    }
}