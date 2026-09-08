package mx.bastekor.flowweaver.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.config.FlowWeaverRootConfig;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.context.AuditTrailContainer;
import mx.bastekor.flowweaver.context.BusinessLogContainer;
import mx.bastekor.flowweaver.service.IFlowWeaverAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static mx.bastekor.flowweaver.context.FlowWeaverContext.assignBusinessLogContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.clearBusinessLogContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.createAuditTrailContainer;
import static mx.bastekor.flowweaver.enums.StatusEnum.SOURCE_FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SOURCE_SUCCESS;
import static mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapArgs;
import static mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapObject;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class FlowWeaverAspect {

    private final FlowWeaverRootConfig flowWeaverRootConfig;
    private final IFlowWeaverAspectService flowWeaverAspectService;

    /**
     * Aspecto para @BusinessLog
     * Crea el BusinessLog padre y lo establece en el contexto
     */
    @Around("@annotation(businessLog)")
    public Object aroundBusinessLog(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {

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
            throw throwable;
        } finally {
            blc.setStatus(status);
            blc.setExitSignature(mapArgs(joinPoint, flowWeaverRootConfig.getMaxDepth()));
            blc.setResponse(mapObject(response, flowWeaverRootConfig.getMaxDepth()));
            blc.setBusinessLog(businessLog);
            flowWeaverAspectService.processBusinessLog(blc);
            clearBusinessLogContainer(blc.getCode());
        }
    }

    /**
     * Aspecto para @AuditTrail
     * Se registra en el BusinessLog padre si existe, si no, crea uno "padrastro"
     */
    @Around("@annotation(auditTrail)")
    public Object aroundAuditTrail(ProceedingJoinPoint joinPoint, AuditTrail auditTrail) throws Throwable {

        final BusinessLogContainer blc = assignBusinessLogContainer(auditTrail.group(), auditTrail.parentCode());
        final AuditTrailContainer inAuditTrailContainer = createAuditTrailContainer(blc, auditTrail.code());
        StatusEnum status = SOURCE_SUCCESS;
        inAuditTrailContainer.setStatus(status);
        inAuditTrailContainer.setAuditTrail(auditTrail);
        inAuditTrailContainer.setEntrySignature(mapArgs(joinPoint, flowWeaverRootConfig.getMaxDepth()));
        flowWeaverAspectService.processAuditTrail(inAuditTrailContainer);

        // Se crea el objeto de salida antes de invocar al método anotado para obtener duración.
        final AuditTrailContainer outAuditTrailContainer = createAuditTrailContainer(blc, auditTrail.code());
        Object response = null;
        try {
            response = joinPoint.proceed();
            return response;
        } catch (Throwable throwable) {
            status = SOURCE_FAILURE;
            response = throwable;
            throw throwable;
        } finally {
            outAuditTrailContainer.setStatus(status);
            outAuditTrailContainer.setAuditTrail(auditTrail);
            outAuditTrailContainer.setResponse(mapObject(response, flowWeaverRootConfig.getMaxDepth()));
            outAuditTrailContainer.setExitSignature(mapArgs(joinPoint, flowWeaverRootConfig.getMaxDepth()));
            flowWeaverAspectService.processAuditTrail(outAuditTrailContainer);

            // Eliminamos el "BusinessLogContainer" creado únicamente para estos "AuditTrailContainer"s.
            // Siempre se limpia el del BusinessLog padre (cubriendo también huérfanos sin parentCode).
            if (isBlank(auditTrail.parentCode())) {
                clearBusinessLogContainer(blc.getCode());
            }
        }
    }
}