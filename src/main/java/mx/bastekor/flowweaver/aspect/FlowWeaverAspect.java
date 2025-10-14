package mx.bastekor.flowweaver.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.context.FlowWeaverContext;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.model.ThreadContainer;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_START;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_START;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.existBusinessLogContainerInCurrentThread;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.getBusinessLogContainerInCurrentThread;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.getCurrentThreadContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.setBusinessLogContainerInCurrentThread;
import static mx.bastekor.flowweaver.enums.StatusEnum.FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SUCCESS;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.buildBusinessLogEvent;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class FlowWeaverAspect {

    private static final String BUSINESS_LOG_PREFIX = "BL";
    private static final String AUDIT_TRAIL_PREFIX = "AT";

    private final IBusinessLogAspectService businessLogAspectService;

    /**
     * Aspecto para @BusinessLog
     * Crea el BusinessLog padre y lo establece en el contexto
     */
    @Around("@annotation(businessLog)")
    public Object aroundBusinessLog(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {

        log.info(BUSINESS_LOG_START);
        BusinessLogContainer businessLogContainer = this.createOrRetrieveBusinessLogContainer(businessLog.operationCode());
        log.debug("🏁 [BusinessLog START] [{}|{}] | Thread: {}",
                businessLogContainer.getOperationCode(),
                businessLogContainer.getFlowId(),
                Thread.currentThread().getName());
        StatusEnum status = null;
        Object output = null;
        Throwable exception = null;
        try {
            status = SUCCESS;
            output = joinPoint.proceed();
            return output;
        } catch (Throwable throwable) {
            status = FAILURE;
            exception = throwable;
            log.error("❌ [BusinessLog ERROR] [{}|{}] | Error: {}",
                    businessLogContainer.getOperationCode(),
                    businessLogContainer.getFlowId(),
                    throwable.getMessage());
            throw throwable;
        } finally {
            // Lo siguiente no debería por ninguna razón fallar ya que es la data "estática" no tratada.
            BusinessLogEvent event = buildBusinessLogEvent(joinPoint, businessLog, status, output, exception, businessLogContainer);
            businessLogAspectService.processBusinessLog(event, status, businessLogContainer.getFlowId());
            log.debug("🏁 [BusinessLog END] [{}|{}] | Duration: {} | AuditTrails: {} | Thread: {}",
                    businessLogContainer.getOperationCode(),
                    businessLogContainer.getFlowId(),
                    businessLogContainer.getDuration(),
                    businessLogContainer.getAuditTrails().size(),
                    Thread.currentThread().getName());
            log.info(BUSINESS_LOG_END, status);
            // Eliminar BusinessLogContainer del contexto del thread y en caso de ser el último, limpiar el pool
            this.clearBusinessLogInCurrentThread(businessLogContainer.getOperationCode());
        }
    }

    /**
     * Aspecto para @AuditTrail
     * Se registra en el BusinessLog padre si existe, si no, crea uno "padrastro"
     */
    @Around("@annotation(auditTrail)")
    public Object aroundAuditTrail(ProceedingJoinPoint joinPoint, AuditTrail auditTrail) throws Throwable {

        log.info(AUDIT_TRAIL_START);
        BusinessLogContainer businessLogContainer = this.createOrRetrieveBusinessLogContainer(auditTrail.flowCode());
        // Código del flujo "AuditTrail" que registra el flujo funcional
        String operationCode = auditTrail.operationCode();
        if (isBlank(operationCode)) {
            operationCode = generate(AUDIT_TRAIL_PREFIX);
        }

        // Crear AuditTrail, hijo de entrada...
        AuditTrailContainer auditTrailContainerIn = new AuditTrailContainer(businessLogContainer.getOperationCode(), operationCode);
        log.debug("  ▶️ [AuditTrail ENTRADA] [{}|{}] | BusinessLog: {} | Duration: {} | Thread: {}",
                auditTrailContainerIn.getOperationCode(),
                auditTrailContainerIn.getFlowId(),
                businessLogContainer.getOperationCode(),
                auditTrailContainerIn.getDuration(),
                Thread.currentThread().getName());
        // Registrar el AuditTrail hijo de entrada en el BusinessLog padre
        businessLogContainer.addAuditTrail(auditTrailContainerIn);

        try {
            // Crear AuditTrail, hijo de salida, se crea antes de soltar el flujo
            AuditTrailContainer auditTrailContainerOut = new AuditTrailContainer(businessLogContainer.getOperationCode(), operationCode,
                    auditTrailContainerIn.getFlowId());
            Object result = joinPoint.proceed();
            log.debug("  ◀️ [AuditTrail SALIDA] [{}|{}] | BusinessLog: {} | Duration: {} | Thread: {}",
                    auditTrailContainerOut.getOperationCode(),
                    auditTrailContainerOut.getFlowId(),
                    businessLogContainer.getOperationCode(),
                    auditTrailContainerOut.getDuration(),
                    Thread.currentThread().getName());
            // Registrar el AuditTrail hijo de salida en el BusinessLog padre
            businessLogContainer.addAuditTrail(auditTrailContainerOut);
            return result;
        } catch (Throwable throwable) {
            log.error("  ❌ [AuditTrail ERROR] [{}|{}] | Duration: {} | Error: {}",
                    auditTrailContainerIn.getOperationCode(), auditTrailContainerIn.getFlowId(),
                    auditTrailContainerIn.getDuration(), throwable.getMessage());
            throw throwable;
        } finally {
            if (isBlank(auditTrail.flowCode())) {
                this.clearBusinessLogInCurrentThread(businessLogContainer.getOperationCode());
            }
        }
    }

    private BusinessLogContainer createOrRetrieveBusinessLogContainer(String operationCode) {

        BusinessLogContainer businessLogContainer = null;
        if (isBlank(operationCode)) {
            // Buscar BusinessLogContainer en el pool mediante PREFIX
            ThreadContainer threadContainer = getCurrentThreadContainer(); // Si no existe se crea contexto del hilo
            for (BusinessLogContainer businessLog : threadContainer.getBusinessLogs()) {
                if (businessLog.getOperationCode().startsWith(BUSINESS_LOG_PREFIX)) {
                    businessLogContainer = businessLog;
                    // Este es el que llamaremos el padrastro...
                    log.warn("👨 [PADRASTRO] Recuperando BusinessLog automático: [{}|{}] para AuditTrail huérfano.",
                            businessLogContainer.getOperationCode(), businessLogContainer.getFlowId());
                    break;
                }
            }
            // Aquí entrará únicamente cuando sea nuevo, pero sin operationCode - default BL#.
            if (businessLogContainer == null) {
                businessLogContainer = new BusinessLogContainer(generate(BUSINESS_LOG_PREFIX));
                // Establecer en el contexto del thread
                setBusinessLogContainerInCurrentThread(businessLogContainer);
            }
        } else { // Si contiene operationCode "BusinessLog"
            // Buscar BusinessLogContainer en el pool por su "operationCode"
            if (existBusinessLogContainerInCurrentThread(operationCode.trim())) {
                businessLogContainer = getBusinessLogContainerInCurrentThread(operationCode.trim());
            } else {
                // Aquí entra únicamente cuando sea nuevo, pero con operationCode.
                businessLogContainer = new BusinessLogContainer(operationCode.trim());
                // Establecer en el contexto del thread
                setBusinessLogContainerInCurrentThread(businessLogContainer);
            }
        }
        return businessLogContainer;
    }

    /**
     * Limpiar el BusinessLog del thread actual.
     *
     * @param operationCode Código de la operación en proceso
     */
    private void clearBusinessLogInCurrentThread(final String operationCode) {
        FlowWeaverContext.clearBusinessLogInCurrentThread(operationCode);
        if (FlowWeaverContext.getSize() == 0) {
            FlowWeaverContext.clearCurrentThread();
        }
    }
}