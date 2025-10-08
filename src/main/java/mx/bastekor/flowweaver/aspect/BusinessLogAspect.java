package mx.bastekor.flowweaver.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_DEBUG_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_DEBUG_START;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_END;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_ERROR;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_RETRY_ERROR;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_START;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.enums.StatusEnum;
import static mx.bastekor.flowweaver.enums.StatusEnum.ERROR;
import static mx.bastekor.flowweaver.enums.StatusEnum.FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SUCCESS;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.buildBusinessLogEvent;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class BusinessLogAspect {

    private final IBusinessLogAspectService businessLogAspectService;

    @Around("@annotation(businessLog)")
    public Object around(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {
        String flowId = FlowWeaverContextHolder.initContextAndMDC();
        log.info(BUSINESS_LOG_START);
        log.debug(BUSINESS_LOG_DEBUG_START, flowId);

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
            throw throwable;
        } finally {
            log.debug(BUSINESS_LOG_DEBUG_END, status, flowId);
            // Lo siguiente no debería por ninguna razón fallar ya que es la data "estática" no tratada.
            BusinessLogEvent event = buildBusinessLogEvent(joinPoint, businessLog, status, output, exception);
            handleBusinessLogEvent(event, flowId);
            FlowWeaverContextHolder.release(); // terminar este "hilo de flujo."
            MDC.remove(FLOW_WEAVER_CONTEXT_ID);
            log.info(BUSINESS_LOG_END, status);
        }
    }

    private void handleBusinessLogEvent(BusinessLogEvent event, String flowId) {
        try {
            System.out.printf("Enviando evento de BusinessLog para flowId: %s\n", flowId);
            System.out.printf("Event: %s\n", event);
            businessLogAspectService.enqueue(event);
        } catch (Exception e) {
            // Error de mi lógica, no debe afectar el flujo normal.
            log.error(BUSINESS_LOG_ERROR, ERROR, flowId, e.getMessage(), e);
            // Intentar reenviar el evento con status ERROR
            try {
                event.setStatus(ERROR);
                businessLogAspectService.enqueue(event);
            } catch (Exception retryException) {
                log.error(BUSINESS_LOG_RETRY_ERROR, retryException.getMessage());
            }
        }
    }
}