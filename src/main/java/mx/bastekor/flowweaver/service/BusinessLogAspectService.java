package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.BusinessLogConfig;
import mx.bastekor.flowweaver.config.InfoAppConfig;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.generateOperationCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessLogAspectService implements IBusinessLogAspectService {

    /**
     * Configuración de la sección "info.app" en el archivo de propiedades.
     */
    private final InfoAppConfig infoAppConfig;

    /**
     * Configuración tomada del archivo de propiedades en la sección
     * "flow-weaver.business-logs"
     */
    private final BusinessLogConfig businessLogConfig;

    /**
     * Configuración "global" del servicio.
     */
    private final Environment environment;

    @Override
    @Async("flowWeaverExecutor")
    public void enqueue(BusinessLogEvent businessLogEvent) {
        // Se propaga el contexto de flujo a la nueva tarea.
        MDC.put(FLOW_WEAVER_CONTEXT_ID, businessLogEvent.getFlowWeaverContextId());
        // Se crea "operationCode" si es que no lo tiene.
        businessLogEvent.getBusinessLogDTO().setOperationCode(generateOperationCode(businessLogEvent));
        // Armar DTO a mandar para trazabilidad.

        // Se manda DtoRequest a la cola de trazabilidad y se espera la respuesta DtoResponse.
        // Se procesa el DtoResponse quien contendrá la lógica de trazabilidad que se envío/logueo.
        this.logData(businessLogEvent);
    }

    private void logData(BusinessLogEvent businessLogEvent) {
        log.info("[AUDIT] Información de la Aplicación: {}", infoAppConfig);
        log.info("[AUDIT] Entrando evento: {}", businessLogEvent);
        log.info("[AUDIT] Configuraciones: {}", businessLogConfig);

        /*
         * Aquí deberíamos de procesar todos los datos y transformarlos en objetos planos reutilizables
         * fuera de la lógia prevía en donde se obtuvieron los datos.
         */
    }
}