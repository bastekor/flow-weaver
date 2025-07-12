package mx.bastekor.flowweaver.aspect;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.enums.StatusEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class AuditTrailAspect {

    @Around("@annotation(auditTrail)")
    public Object around(ProceedingJoinPoint joinPoint, AuditTrail auditTrail) throws Throwable {
        Instant start = Instant.now();
        FlowWeaverContextHolder.initOrReuse();
        String flowId = FlowWeaverContextHolder.getFlowId();
        MDC.put(FLOW_WEAVER_CONTEXT_ID, flowId);

        StatusEnum status = null;
        Object output = null;
        Throwable exception = null;

        try {
            status = StatusEnum.SUCCESS;
            log.info("[{}] AuditTrail - Entrada - {}", flowId, auditTrail.operationCode());
            output = joinPoint.proceed();
            return output;
        } catch (Throwable throwable) {
            status = StatusEnum.FAILURE;
            exception = throwable;
            throw throwable;
        } finally {
            Instant end = Instant.now();
            log.info("[{}] AuditTrail - Salida-{} - {}", flowId, status.name(), auditTrail.operationCode());
            FlowWeaverContextHolder.release();
        }
    }
}