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

import java.time.Instant;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.buildBusinessLogEvent;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class BusinessLogAspect {

    private final IBusinessLogAspectService businessLogAspectService;

    @Around("@annotation(businessLog)")
    public Object around(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {
        Instant start = Instant.now();
        FlowWeaverContextHolder.initOrReuse();
        String flowId = FlowWeaverContextHolder.getFlowId();
        MDC.put(FLOW_WEAVER_CONTEXT_ID, flowId);

        StatusEnum status = null;
        Object output = null;
        Throwable exception = null;

        try {
            status = StatusEnum.SUCCESS;
            output = joinPoint.proceed();
            return output;
        } catch (Throwable throwable) {
            status = StatusEnum.FAILURE;
            exception = throwable;
            throw throwable;
        } finally {
            Instant end = Instant.now();
            BusinessLogEvent event = buildBusinessLogEvent(joinPoint, businessLog, start, end, status, output, exception);
            businessLogAspectService.enqueue(event);
            log.info("[{}] BusinessLog - Salida({})", flowId, status.name());
            FlowWeaverContextHolder.clear();
        }
    }
}