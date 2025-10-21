package mx.bastekor.flowweaver.aspect;

import static mx.bastekor.flowweaver.util.BusinessLogUtils.createMethodContext;
import static org.apache.commons.lang3.StringUtils.isBlank;

import mx.bastekor.flowweaver.util.BusinessLogUtils;
import mx.bastekor.flowweaver.util.Util;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.weaver.Utils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_START;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_START;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.assignBusinessLogContainer;
import static mx.bastekor.flowweaver.context.FlowWeaverContext.clearBusinessLogContainer;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.enums.StatusEnum;
import static mx.bastekor.flowweaver.enums.StatusEnum.FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SUCCESS;
import static mx.bastekor.flowweaver.mapper.AuditTrailMapper.createAuditTrailDTO;
import static mx.bastekor.flowweaver.mapper.BusinessLogMapper.createBusinessLogDTO;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;

import java.time.Instant;

import static mx.bastekor.flowweaver.util.BusinessLogUtils.buildBusinessLogEvent;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;

@Slf4j
@Aspect
@Order(1)
@Component
@RequiredArgsConstructor
public class FlowWeaverAspect {

    private static final String BUSINESS_LOG_PREFIX = "BL#";
    private static final String AUDIT_TRAIL_PREFIX = "AT#";

    private final IBusinessLogAspectService businessLogAspectService;

    /**
     * Aspecto para @BusinessLog
     * Crea el BusinessLog padre y lo establece en el contexto
     */
    @Around("@annotation(businessLog)")
    public Object aroundBusinessLog(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {

        log.info(BUSINESS_LOG_START);

        Instant start = Instant.now();
        final BusinessLogDTO businessLogDTO = createBusinessLogDTO(businessLog);
        final BusinessLogContainer blc = assignBusinessLogContainer(businessLogDTO.getOperationCode());
        businessLogDTO.setOperationCode(blc.getOperationCode());


        this.printContainer(blc, 1);

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
            log.error("❌ [BusinessLog ERROR] [{}|{}] | Error: {}", blc.getFlowId(), blc.getOperationCode(), throwable.getMessage());
            throw throwable;
        } finally {

            BusinessLogEvent businessLogEvent = new BusinessLogEvent();
            businessLogEvent.setFlowWeaverContextId(blc.getFlowId());
            businessLogEvent.setDuration(blc.getDuration());
            businessLogEvent.setStatus(status);
            businessLogEvent.setMethodContext(createMethodContext(joinPoint, output, exception));

            Instant end =  Instant.now();
            log.info("BSTK : TIME_DURATION :: {} - Object Content :: {}", Util.getDuration(start, end), businessLogEvent);

// Lo siguiente no debería por ninguna razón fallar ya que es la data "estática" no tratada.
            BusinessLogEvent event = buildBusinessLogEvent(joinPoint, businessLog, status, output, exception, blc);
            /*
            Aquí deberíamos de scar toda la información o mandar a extraer a otro lado
             */
            businessLogAspectService.processBusinessLog(businessLogEvent);
            this.printContainer(blc, 2);
            // Eliminar BusinessLogContainer del contexto del thread y en caso de ser el último, limpiar el pool
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

        final AuditTrailDTO auditTrailDTO = createAuditTrailDTO(auditTrail);
        final BusinessLogContainer businessLogContainer = assignBusinessLogContainer(auditTrailDTO.getFlowCode());

        // Código del flujo "AuditTrail" que registra el flujo funcional
        String operationCode = auditTrail.operationCode();
        if (isBlank(operationCode)) {
            operationCode = generate(AUDIT_TRAIL_PREFIX);
        }

        /**
         * revisar el orden de está madre porque hasta donde recuerdo en .proceed() suelta el flujo y nos permite
         * continuar con nuestro desma, más no se si se sigue en otro momento, creo que no porque se espera a que
         * termine el flujo.
         */





        // Se tiene que mandar toda la mierda a obtener y procesarla para soltar lo más rápido posible el flujo.
        AuditTrailContainer auditTrailContainerIn = this.beforeAuditTrail(operationCode, businessLogContainer);

        StatusEnum status = null;
        Object output = null;
        Throwable exception = null;

        try {
            // Se tiene que mandar toda la mierda a obtener y procesarla para soltar lo más rápido posible el flujo.
            AuditTrailContainer auditTrailContainerOut =
                    this.afterAuditTrail(operationCode, auditTrailContainerIn, businessLogContainer);
            status = SUCCESS;
            // Enviar a logs entrada antes de soltar el flujo
            // Hay que ver como refactorizamos esto para que entre con audittrail
            BusinessLogEvent in = buildBusinessLogEvent(joinPoint, null, status, output, exception, businessLogContainer);
            output = joinPoint.proceed();
            return output;
        } catch (Throwable throwable) {
            status = FAILURE;
            exception = throwable;
            log.error("  ❌ [AuditTrail ERROR] [{}|{}] | Duration: {} | Error: {}",
                    auditTrailContainerIn.getOperationCode(), auditTrailContainerIn.getFlowId(),
                    auditTrailContainerIn.getDuration(), throwable.getMessage());
            throw throwable;
        } finally {

            // Enviar a logs salida antes de soltar el flujo
            // Hay que ver como refactorizamos esto para que entre con audittrail
            BusinessLogEvent out = buildBusinessLogEvent(joinPoint, null, status, output, exception, businessLogContainer);


            // Único para "BusinessLogContainer" por default, ya que elimina al BusinessLogContainer creado
            // temporalmente para este "huerfano".
            if (isBlank(auditTrail.flowCode())) {
                clearBusinessLogContainer(businessLogContainer.getOperationCode());
            }
        }
    }


    private AuditTrailContainer beforeAuditTrail(String operationCode, BusinessLogContainer businessLogContainer) {
        log.info(AUDIT_TRAIL_START);
        // Crear AuditTrail, hijo de entrada...
        AuditTrailContainer auditTrailContainer = new AuditTrailContainer(businessLogContainer.getOperationCode(), operationCode);
        log.debug("  ▶️ [AuditTrail ENTRADA] [{}|{}] | BusinessLog: {} | Duration: {} | Thread: {}",
                auditTrailContainer.getOperationCode(),
                auditTrailContainer.getFlowId(),
                businessLogContainer.getOperationCode(),
                auditTrailContainer.getDuration(),
                Thread.currentThread().getName());
        // Registrar el AuditTrail hijo de entrada en el BusinessLog padre
        businessLogContainer.addAuditTrail(auditTrailContainer);
        log.info(AUDIT_TRAIL_END);
        return auditTrailContainer;
    }

    private AuditTrailContainer afterAuditTrail(String operationCode,
                                                AuditTrailContainer auditTrailContainerIn,
                                                BusinessLogContainer businessLogContainer) {
        log.info(AUDIT_TRAIL_START);
        // Crear AuditTrail, hijo de salida, se crea antes de soltar el flujo
        AuditTrailContainer auditTrailContainer = new AuditTrailContainer(businessLogContainer.getOperationCode(), operationCode,
                auditTrailContainerIn.getFlowId());
        log.debug("  ◀️ [AuditTrail SALIDA] [{}|{}] | BusinessLog: {} | Duration: {} | Thread: {}",
                auditTrailContainer.getOperationCode(),
                auditTrailContainer.getFlowId(),
                businessLogContainer.getOperationCode(),
                auditTrailContainer.getDuration(),
                Thread.currentThread().getName());
        // Registrar el AuditTrail hijo de salida en el BusinessLog padre
        businessLogContainer.addAuditTrail(auditTrailContainer);
        log.info(AUDIT_TRAIL_END);
        return auditTrailContainer;
    }

    private void printContainer(BusinessLogContainer businessLogContainer, int input) {

        switch (input) {
            case 1:
                log.debug("🏁 [BusinessLog START] [{}|{}] | Thread: {}",
                        businessLogContainer.getOperationCode(),
                        businessLogContainer.getFlowId(),
                        Thread.currentThread().getName());
                break;
            case 2:
                log.debug("🏁 [BusinessLog END] [{}|{}] | Duration: {} | AuditTrails: {} | Thread: {}",
                        businessLogContainer.getOperationCode(),
                        businessLogContainer.getFlowId(),
                        businessLogContainer.getDuration(),
                        businessLogContainer.getAuditTrails().size(),
                        Thread.currentThread().getName());
                break;
        }
    }
}