package mx.bastekor.flowweaver.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_PREFIX;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_START;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.addAuditTrailContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.assignBusinessLogContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.clearBusinessLogContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.printRecursive;
import static mx.bastekor.flowweaver.enums.StatusEnum.FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SUCCESS;
import static mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapArgs;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trim;

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

    private final IBusinessLogAspectService businessLogAspectService;

    /**
     * Aspecto para @BusinessLog
     * Crea el BusinessLog padre y lo establece en el contexto
     */
    @Around("@annotation(businessLog)")
    public Object aroundBusinessLog(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {

        log.info(BUSINESS_LOG_START);
        final BusinessLogContainer blc = assignBusinessLogContainer(trim(businessLog.operationCode()));

        StatusEnum status = null;
        Object response = null;
        try {
            status = SUCCESS;
            response = joinPoint.proceed();
            return response;
        } catch (Throwable throwable) {
            status = FAILURE;
            response = throwable;
            log.error("❌ [BusinessLog ERROR] [{}|{}] | Error: {}", blc.getOperationId(), blc.getOperationCode(), throwable.getMessage());
            throw throwable;
        } finally {
            blc.setStatus(status);
            blc.setResponse(response);
            blc.setOutputData(mapArgs(joinPoint, maxDepth));
            blc.setBusinessLog(businessLog);

            try {
                businessLogAspectService.processBusinessLog(blc);
            } catch (Exception exc) {
                log.error("ERROR A TRATAR, NO ERROR DE FLOW SINO DE PROCESO - Error procesando BusinessLog: {}", exc.getMessage());
                // Deberemos de mandar a log datos iniciales mas errores de exc...
            }

            printRecursive(bool);
            // Eliminar BusinessLogContainer del contexto del thread.
            clearBusinessLogContainer(blc.getOperationCode());
            log.info(BUSINESS_LOG_END, status);
        }
    }

    /**
     * Aspecto para @AuditTrail
     * Se registra en el BusinessLog padre si existe, si no, crea uno "padrastro"
     */
    @Around("@annotation(auditTrail)")
    public Object aroundAuditTrail(ProceedingJoinPoint joinPoint, AuditTrail auditTrail) throws Throwable {

        final BusinessLogContainer blc = assignBusinessLogContainer(trim(auditTrail.flowCode()));
        final String flowCode = blc.getOperationCode();
        final String flowId = UUID.randomUUID().toString();

        // Código del flujo "AuditTrail" que registra el flujo funcional
        String operationCode = auditTrail.operationCode();
        if (isBlank(operationCode)) {
            operationCode = generate(AUDIT_TRAIL_PREFIX);
        }

        final AuditTrailContainer atcIn = new AuditTrailContainer(flowCode, operationCode, flowId);
        final String snapshotIn = mapArgs(joinPoint, maxDepth);
        addAuditTrailContainer(0, atcIn, blc);
        businessLogAspectService.processAuditTrailIn(auditTrail, snapshotIn, atcIn);

        StatusEnum status = SUCCESS;
        Object response = null;
        Throwable exception = null;

        // Se crea el objeto de salida antes de invocar al método anotado para obtener duración.
        final AuditTrailContainer atcOut = new AuditTrailContainer(flowCode, operationCode, flowId);
        addAuditTrailContainer(1, atcOut, blc);
        try {
            response = joinPoint.proceed();
            return response;
        } catch (Throwable throwable) {
            status = FAILURE;
            exception = throwable;
            log.error("  ❌ [AuditTrail ERROR] [{}|{}] | Duration: {} | Error: {}",
                    atcOut.getOperationCode(), atcOut.getFlowId(),
                    atcOut.getDuration(), throwable.getMessage());
            throw throwable;
        } finally {
            final String snapshotOut = mapArgs(joinPoint, maxDepth);
            businessLogAspectService.processAuditTrailOut(auditTrail, snapshotOut, status, response, exception, atcOut);
            // Único para "BusinessLogContainer" por default, ya que elimina al BusinessLogContainer creado
            // temporalmente para este "huerfano".
            if (isBlank(auditTrail.flowCode())) {
                clearBusinessLogContainer(blc.getOperationCode());
            }
        }
    }
}