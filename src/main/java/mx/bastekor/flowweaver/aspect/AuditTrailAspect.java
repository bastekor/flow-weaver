package mx.bastekor.flowweaver.aspect;

import java.time.Instant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_ENTRY;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_EXIT;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.enums.StatusEnum;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class AuditTrailAspect {

    @Around("@annotation(auditTrail)")
    public Object around(ProceedingJoinPoint joinPoint, AuditTrail auditTrail) throws Throwable {
        Instant start = Instant.now();
        String flowId = FlowWeaverContextHolder.initContextAndMDC();

        StatusEnum status = null;
        Object output = null;
        Throwable exception = null;

        try {
            status = StatusEnum.SUCCESS;
            log.info("[{}] {} {}", flowId, AUDIT_TRAIL_ENTRY, auditTrail.operationCode());
            output = joinPoint.proceed();
            return output;
        } catch (Throwable throwable) {
            status = StatusEnum.FAILURE;
            exception = throwable;
            throw throwable;
        } finally {
            Instant end = Instant.now();
            log.info("[{}] {} - {}", flowId, AUDIT_TRAIL_EXIT, auditTrail.operationCode());
            FlowWeaverContextHolder.release();
        }
    }
}