package mx.bastekor.flowweaver.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.context.FlowWeaverContext;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;
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
            Thread.sleep(10000); // Hay que eliminar
            return output;
        } catch (Throwable throwable) {
            status = FAILURE;
            exception = throwable;
            throw throwable;
        } finally {
            try {
                log.debug("[{}] - BusinessLog - Entrada({})", flowId, status.name());
                BusinessLogEvent event = buildBusinessLogEvent(joinPoint, businessLog, status, output, exception);
                this.testMethod(event);
                businessLogAspectService.enqueue(event);
                log.debug("[{}] - BusinessLog - Salida({})", flowId, status.name());
                FlowWeaverContextHolder.release();
                log.info("[{}] - End around BusinessLogAspect", flowId);
                FlowWeaverContextHolder.clear(); // Este esta demás por el release, ese ya hace el remove también
                this.testMethod(event);
            } catch (Exception e) {
                // Error de mi lógica, no debe afectar el flujo normal.
                log.error("[{}] - End around BusinessLogAspect::Err({})", flowId, e.getMessage(), e);
            } finally {
                // Hay que averiguar si puede fallar en el flujo mas adelante para saber cuando se manda y cuando no.
                // Además de saber qué datos si o sí siempre se podrán obtener que "nunca" causarán errores de lógica
                // de programación
            }
        }
    }


    // Los metodos de abajo se deben de borrar
    private void testMethod(BusinessLogEvent businessLogEvent) {
        ObjectMapper objectMapper = new ObjectMapper();
        FlowWeaverContext flowWeaverContext = FlowWeaverContextHolder.get();
        String json = convertToJson(businessLogEvent);
        String json2 = convertToJson(flowWeaverContext);
        log.debug("JSON :: {}", json);
        log.debug("JSON2 :: {}", json2);
    }

    private String convertToJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return Objects.toString(object);
        }
    }
}