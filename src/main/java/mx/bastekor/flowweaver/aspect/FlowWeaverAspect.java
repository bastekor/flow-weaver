package mx.bastekor.flowweaver.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.service.IFlowWeaverAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_ERROR;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_PREFIX;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_START;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_ERROR;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_START;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.addAuditTrailContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.assignBusinessLogContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.clearBusinessLogContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.printRecursive;
import static mx.bastekor.flowweaver.enums.StatusEnum.SOURCE_FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SOURCE_SUCCESS;
import static mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapArgs;
import static mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapObject;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class FlowWeaverAspect {

    @Value("${flow-weaver.debug.recursive:false}")
    private boolean bool;

    @Value("${flow-weaver.max-depth:3}")
    private int maxDepth;

    private final IFlowWeaverAspectService flowWeaverAspectService;

    /**
     * Aspecto para @BusinessLog
     * Crea el BusinessLog padre y lo establece en el contexto
     */
    @Around("@annotation(businessLog)")
    public Object aroundBusinessLog(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {
        log.info(BUSINESS_LOG_START);

        final BusinessLogContainer blc = assignBusinessLogContainer(businessLog.group(), businessLog.code());
        StatusEnum status = null;
        Object response = null;
        try {
            status = SOURCE_SUCCESS;
            response = joinPoint.proceed();
            return response;
        } catch (Throwable throwable) {
            status = SOURCE_FAILURE;
            response = throwable;
            log.error(BUSINESS_LOG_ERROR, blc.getCorrelationId(), blc.getId(), blc.getCode(), throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            blc.setStatus(status);
            blc.setExitSignature(mapArgs(joinPoint, maxDepth));
            blc.setResponse(mapObject(response, maxDepth));
            blc.setBusinessLog(businessLog);
            flowWeaverAspectService.processBusinessLog(blc);
            printRecursive(bool);
            clearBusinessLogContainer(blc.getCode());
            log.info(BUSINESS_LOG_END, status);
        }
    }

    /**
     * Aspecto para @AuditTrail
     * Se registra en el BusinessLog padre si existe, si no, crea uno "padrastro"
     */
    @Around("@annotation(auditTrail)")
    public Object aroundAuditTrail(ProceedingJoinPoint joinPoint, AuditTrail auditTrail) throws Throwable {
        log.info(AUDIT_TRAIL_START);

        final AuditTrailContainer inAuditTrailContainer = new AuditTrailContainer();

        final BusinessLogContainer blc = assignBusinessLogContainer(auditTrail.group(), auditTrail.parentCode());
        final String blcGroup = blc.getGroup();
        final String blcCode = blc.getCode();
        final String blcCorrelationId = blc.getCorrelationId();
        final String code = defaultIfBlank(auditTrail.code(), generate(AUDIT_TRAIL_PREFIX));

        this.fillAuditTrailContainer(blcGroup, blcCode, blcCorrelationId, code, auditTrail, inAuditTrailContainer);
        inAuditTrailContainer.setEntrySignature(mapArgs(joinPoint, maxDepth));
        inAuditTrailContainer.setStatus(SOURCE_SUCCESS);
        addAuditTrailContainer(true, inAuditTrailContainer, blc);
        flowWeaverAspectService.processAuditTrail(inAuditTrailContainer);

        // Se crea el objeto de salida antes de invocar al método anotado para obtener duración.
        final AuditTrailContainer outAuditTrailContainer = new AuditTrailContainer();
        StatusEnum status = null;
        Object response = null;
        try {
            status = SOURCE_SUCCESS;
            response = joinPoint.proceed();
            return response;
        } catch (Throwable throwable) {
            status = SOURCE_FAILURE;
            response = throwable;
            log.error(AUDIT_TRAIL_ERROR, blcCorrelationId, code, outAuditTrailContainer.getDuration(),
                    throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            this.fillAuditTrailContainer(blcGroup, blcCode, blcCorrelationId, code, auditTrail, outAuditTrailContainer);
            outAuditTrailContainer.setExitSignature(mapArgs(joinPoint, maxDepth));
            outAuditTrailContainer.setResponse(mapObject(response, maxDepth));
            outAuditTrailContainer.setStatus(status);
            addAuditTrailContainer(false, outAuditTrailContainer, blc);
            flowWeaverAspectService.processAuditTrail(outAuditTrailContainer);

            // Eliminamos el "BusinessLogContainer" creado únicamente para estos "AuditTrailContainer"s.
            if (isBlank(auditTrail.parentCode())) {
                clearBusinessLogContainer(blc.getCode());
            }
            log.info(AUDIT_TRAIL_END, status);
        }
    }

    private void fillAuditTrailContainer(String groupCode, String parentCode, String correlationId, String code,
                                         AuditTrail auditTrail, AuditTrailContainer auditTrailContainer) {
        auditTrailContainer.setGroup(groupCode);
        auditTrailContainer.setParentCode(parentCode);
        auditTrailContainer.setCorrelationId(correlationId);
        auditTrailContainer.setCode(code);
        auditTrailContainer.setAuditTrail(auditTrail);
    }

}