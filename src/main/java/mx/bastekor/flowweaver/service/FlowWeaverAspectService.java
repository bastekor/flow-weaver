package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.FlowWeaverRootConfig;
import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.exception.FlowWeaverException;
import mx.bastekor.flowweaver.handler.FlowWeaverResultHandler;
import mx.bastekor.flowweaver.mapper.RequestDTOMapper;
import mx.bastekor.flowweaver.context.AuditTrailContainer;
import mx.bastekor.flowweaver.context.BusinessLogContainer;
import mx.bastekor.flowweaver.dto.FlowWeaverResponse;
import mx.bastekor.flowweaver.util.FrameExtractor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FIELDS_IS_NULL_OR_EMPTY;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.PROCESS_STATUS;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.REQUEST_ISNULL;
import static mx.bastekor.flowweaver.enums.OutputMode.DUAL_LINE;
import static mx.bastekor.flowweaver.enums.StatusEnum.INTERNAL_ERROR;
import static mx.bastekor.flowweaver.enums.StatusEnum.INTERNAL_FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.INTERNAL_SUCCESS;
import static mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.toMethodSnapshot;
import static mx.bastekor.flowweaver.util.Util.getDuration;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowWeaverAspectService implements IFlowWeaverAspectService {
    private static final int MAX_RECOVERIES = 3;
    private static final String ERROR_MESSAGE = "Error '{}' no manejado, message={}";

    private final FrameConfig frameConfig;
    private final FrameExtractor frameExtractor;
    private final RequestDTOMapper requestDTOMapper;
    private final FlowWeaverRootConfig flowWeaverRootConfig;
    private final FlowWeaverResultHandler flowWeaverResultHandler;

    @Override
    @Async("flowWeaverExecutor")
    public void processBusinessLog(final BusinessLogContainer businessLogContainer) {
        final String methodDuration = businessLogContainer.getDuration();
        this.sendHandler(methodDuration, businessLogContainer);
    }

    @Override
    @Async("flowWeaverExecutor")
    public void processAuditTrail(final AuditTrailContainer auditTrailContainer) {
        final String methodDuration = auditTrailContainer.getDuration();
        this.sendHandler(methodDuration, auditTrailContainer);
    }

    private void sendHandler(final String methodDuration, final Object object) {
        final Instant start = Instant.now();

        final FlowWeaverResponse rs = new FlowWeaverResponse();

        if (object instanceof BusinessLogContainer container) {
            rs.setMethodSignature(container.getExitSignature());
            rs.setMethodResponse(container.getResponse());
            rs.setAnnotationDTO(requestDTOMapper.resolveBusinessLog(container));
            rs.setRequestDTO(requestDTOMapper.build(container, rs.getAnnotationDTO()));
        }

        if (object instanceof AuditTrailContainer container) {
            rs.setMethodSignature(defaultIfBlank(container.getEntrySignature(), container.getExitSignature()));
            rs.setMethodResponse(container.getResponse());
            rs.setAnnotationDTO(requestDTOMapper.resolveAuditTrail(container));
            rs.setRequestDTO(requestDTOMapper.build(container, (AuditTrailDTO) rs.getAnnotationDTO()));
        }
        // Retorna mapa vacío si no encuentra datos a extraer
        Map<String, Object> fields = frameExtractor.extract(rs.getRequestDTO(), flowWeaverRootConfig.getMaxDepth());
        rs.setFields(fields);

        rs.setMethodSnapshotDTO(toMethodSnapshot(rs.getMethodSignature(), rs.getMethodResponse(), methodDuration));

        boolean bool = true;
        int count = 0;
        while (bool && count <= MAX_RECOVERIES) {
            rs.setMappedDuration(getDuration(start, Instant.now()));
            try {
                // Solo la primera vez es correcta, las demás son "fallidas" o "erroneas"
                if (count == 0) {
                    fields.put(PROCESS_STATUS, INTERNAL_SUCCESS);
                }
                flowWeaverResultHandler.handle(rs);
                bool = false;
            } catch (FlowWeaverException e) {
                count++;
                log.error("Error :: {}, message={}", e.getStatus(), e.getMessage());
                fields.put(PROCESS_STATUS, e.getStatus());

                if (e.getStatus() != null && e.getMessage() != null) {
                    if (e.getStatus().equals(INTERNAL_FAILURE)) {
                        if (e.getMessage().equals(REQUEST_ISNULL)) {
                            rs.setRequestDTO(new RequestDTO());
                            rs.getRequestDTO().setId("ID_ERR#");
                            rs.getRequestDTO().setGroup("GC_ERR#");
                            rs.getRequestDTO().setCode("C_ERR#");
                        } else if (e.getMessage().equals(FIELDS_IS_NULL_OR_EMPTY)) {
                            fields.put(PROCESS_STATUS, e.getStatus());
                            rs.setFields(fields);
                        } else {
                            log.error(ERROR_MESSAGE, INTERNAL_FAILURE, e.getMessage(), e);
                            bool = false;
                        }
                    } else if (e.getStatus().equals(INTERNAL_ERROR)) {
                        if (e.getMessage().contains("entrySeparator ('")) {
                            frameConfig.setOutputMode(DUAL_LINE);
                            frameConfig.setEntrySeparator("|");
                            frameConfig.setPairSeparator("=");
                            log.warn("Autocorrección del frameConfig: separadores idénticos no válidos en SINGLE_LINE; se restituye outputMode={}, entrySeparator='{}', pairSeparator='{}'", DUAL_LINE, "|", "=");
                        } else {
                            log.error(ERROR_MESSAGE, INTERNAL_ERROR, e.getMessage(), e);
                            bool = false;
                        }
                    } else {
                        log.error(ERROR_MESSAGE, e.getStatus(), e.getMessage(), e);
                        bool = false;
                    }
                } else {
                    log.error(ERROR_MESSAGE, "(1)", "La excepción no cuenta con información, se ha lanzado sin motivo de uso", e);
                    bool = false;
                }
            } catch (Exception ex) {
                log.error(ERROR_MESSAGE, "(2)", ex.getMessage(), ex);
                bool = false;
            }
        }

        if (bool) {
            log.error("Se agotaron los {} reintentos; no se pudo invocar al FlowWeaverResultHandler.", MAX_RECOVERIES);
        }
    }
}