package mx.bastekor.flowweaver.aspect;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;
import static mx.bastekor.flowweaver.enums.StatusEnum.ERROR;
import static mx.bastekor.flowweaver.enums.StatusEnum.FAILURE;
import static mx.bastekor.flowweaver.enums.StatusEnum.SUCCESS;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.buildBusinessLogEvent;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class BusinessLogAspect {

    private final IBusinessLogAspectService businessLogAspectService;

    @Around("@annotation(businessLog)")
    public Object around(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {
        FlowWeaverContextHolder.initOrReuse();
        String flowId = FlowWeaverContextHolder.getFlowId();
        MDC.put(FLOW_WEAVER_CONTEXT_ID, flowId);
        log.info("[{}] - Start around BusinessLogAspect", flowId);

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
            log.debug("[{}] - BusinessLog - Entrada({})", flowId, status.name());
            // Lo siguiente no debería por ninguna razón fallar ya que es la data "estática" no tratada.
            BusinessLogEvent event = buildBusinessLogEvent(joinPoint, businessLog, status, output, exception);
            try {
                businessLogAspectService.enqueue(event);
                log.debug("[{}] - BusinessLog - Salida({})", flowId, status.name());
                FlowWeaverContextHolder.release();
                log.info("[{}] - End around BusinessLogAspect", flowId);
                FlowWeaverContextHolder.release();
            } catch (Exception e) {
                status = ERROR;
                // Error de mi lógica, no debe afectar el flujo normal.
                log.error("[{}] - End around BusinessLogAspect::Err({})", flowId, e.getMessage(), e);
                // mandemos solo aquello que es posible que se pueda mandar.
                FlowWeaverContextHolder.release();
            }
        }
    }
}