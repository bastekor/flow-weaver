package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.mapper.RequestDTOMapper;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_ERROR;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowWeaverAspectService implements IFlowWeaverAspectService {

    private final Environment environment;
    private final RequestDTOMapper requestDTOMapper;

    @Override
    @Async("flowWeaverExecutor")
    public void processBusinessLog(final BusinessLogContainer businessLogContainer) {
        final String methodDuration = businessLogContainer.getDuration();

        final Instant start = Instant.now();
        try {
            final RequestDTO requestDTO = requestDTOMapper.build(businessLogContainer, environment);

            String frame = getFrame(
                    requestDTO.getId(),
                    "NO_PARENT",
                    requestDTO.getGroup(),
                    requestDTO.getCode(),
                    "EXIT/OUT",
                    requestDTO.getDescription(),
                    requestDTO.getStatus(),
                    requestDTO.getResult(),
                    requestDTO.getAppName(),
                    requestDTO.getAppVersion(),
                    requestDTO.getAppDescription(),
                    requestDTO.getHostName(),
                    requestDTO.getIpAddress()
            );
            log.info("Trama-BusinessLog: {}", frame);
        } catch (Exception e) {
            log.error(BUSINESS_ERROR, businessLogContainer.getStatus(), businessLogContainer.getCode(), e.getMessage(), e);
        } finally {
            final Instant end = Instant.now();
        }
    }

    @Override
    @Async("flowWeaverExecutor")
    public void processAuditTrail(final AuditTrailContainer auditTrailContainer) {
        final String methodDuration = auditTrailContainer.getDuration();

        final Instant start = Instant.now();
        try {
            final RequestDTO requestDTO = requestDTOMapper.build(auditTrailContainer, environment);

            String frame = getFrame(
                    requestDTO.getId(),
                    auditTrailContainer.getParentCode(),
                    requestDTO.getGroup(),
                    requestDTO.getCode(),
                    auditTrailContainer.getEntrySignature() != null ? "ENTRY/IN" : "EXIT/OUT",
                    requestDTO.getDescription(),
                    requestDTO.getResult(),
                    requestDTO.getStatus(),
                    requestDTO.getAppName(),
                    requestDTO.getAppVersion(),
                    requestDTO.getAppDescription(),
                    requestDTO.getHostName(),
                    requestDTO.getIpAddress()
            );
            log.info("Trama-AuditTrail: {}", frame);
        } catch (Exception e) {
            log.error(AUDIT_ERROR, auditTrailContainer.getStatus(), auditTrailContainer.getCorrelationId(), e.getMessage(), e);
        } finally {
            final Instant end = Instant.now();
        }
    }

    private String getFrame(String... args) {
        return String.join("|", args);
    }
}