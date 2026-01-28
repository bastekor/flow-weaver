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
import static mx.bastekor.flowweaver.enums.StatusEnum.FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SUCCESS;
import static mx.bastekor.flowweaver.mapper.SafeSnapshotMapper.mapArgs;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;
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

    private final IBusinessLogAspectService businessLogAspectService;

    /**
     * Aspecto para @BusinessLog
     * Crea el BusinessLog padre y lo establece en el contexto
     */
    @Around("@annotation(businessLog)")
    public Object aroundBusinessLog(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {
        log.info(BUSINESS_LOG_START);

        final BusinessLogContainer blc = assignBusinessLogContainer(businessLog.groupCode(), businessLog.operationCode());
        StatusEnum status = null;
        Object response = null;
        try {
            status = SUCCESS;
            response = joinPoint.proceed();
            return response;
        } catch (Throwable throwable) {
            status = FAILURE;
            response = throwable;
            log.error(BUSINESS_LOG_ERROR, blc.getOperationId(), blc.getOperationCode(), throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            blc.setStatus(status);
            blc.setResponse(response);
            blc.setExitSignature(mapArgs(joinPoint, maxDepth));
            blc.setBusinessLog(businessLog);

            try {
                businessLogAspectService.processBusinessLog(blc);
            } catch (Exception exc) {
                log.error("ERROR A TRATAR, NO ERROR DE FLOW SINO DE PROCESO - Error procesando BusinessLog: {}", exc.getMessage());
                // Deberemos de mandar a log datos iniciales mas errores de exc...

                /*
                 * Aquí se podrá aplicar alguna lógica que permita el envío de datos de entrada y la búsqueda recursiva de las
                 * excepciones anidadas. Quizás con el tiempo se puede implementar algún método que permita saber que datos se
                 * fueron tomando y que datos no, pero eso solo será informativo para mantenimiento de esta dependencia, ya que
                 * errores en lógica interna de como se maneja "x" o "y" cosa es tema propio que no debe de afectar la legibilidad
                 * de la traza que se llegue a mandar, mas bien deberá de complementarla.
                 */
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
        log.info(AUDIT_TRAIL_START);

        final AuditTrailContainer atcIn = new AuditTrailContainer();
        StatusEnum status = null;
        Object response = null;

        final BusinessLogContainer blc = assignBusinessLogContainer(auditTrail.groupCode(), auditTrail.flowCode());
        final String groupCode = blc.getGroupCode();
        final String flowCode = blc.getOperationCode();
        final String flowId = blc.getOperationId();
        final String operationCode = isBlank(auditTrail.operationCode()) ? generate(AUDIT_TRAIL_PREFIX) : auditTrail.operationCode();

        this.fillAuditTrailContainer(groupCode, flowCode, flowId, operationCode, auditTrail, atcIn);
        atcIn.setEntrySignature(mapArgs(joinPoint, maxDepth));
        addAuditTrailContainer(0, atcIn, blc);
        businessLogAspectService.processAuditTrail(atcIn);

        // Se crea el objeto de salida antes de invocar al método anotado para obtener duración.
        final AuditTrailContainer atcOut = new AuditTrailContainer();

        try {
            status = SUCCESS;
            response = joinPoint.proceed();
            return response;
        } catch (Throwable throwable) {
            status = FAILURE;
            response = throwable;
            log.error(AUDIT_TRAIL_ERROR,
                    atcOut.getOperationCode(), atcOut.getFlowId(),
                    atcOut.getDuration(), throwable.getMessage(), throwable);
            throw throwable;
        } finally {
            this.fillAuditTrailContainer(groupCode, flowCode, flowId, operationCode, auditTrail, atcOut);
            atcOut.setExitSignature(mapArgs(joinPoint, maxDepth));
            atcOut.setResponse(response);
            atcOut.setStatus(status);
            addAuditTrailContainer(1, atcOut, blc);
            businessLogAspectService.processAuditTrail(atcOut);

            // Único para "BusinessLogContainer" por default, ya que elimina al BusinessLogContainer creado
            // temporalmente para este "huerfano".
            if (isBlank(auditTrail.flowCode())) {
                clearBusinessLogContainer(blc.getOperationCode());
            }
            log.info(AUDIT_TRAIL_END, status);
        }
    }

    private void fillAuditTrailContainer(String groupCode, String flowCode, String flowId, String operationCode,
                                         AuditTrail auditTrail, AuditTrailContainer auditTrailContainer) {
        auditTrailContainer.setGroupCode(groupCode);
        auditTrailContainer.setFlowCode(flowCode);
        auditTrailContainer.setFlowId(flowId);
        auditTrailContainer.setOperationCode(operationCode);
        auditTrailContainer.setAuditTrail(auditTrail);
    }
}