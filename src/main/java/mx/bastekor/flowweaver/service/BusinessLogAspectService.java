package mx.bastekor.flowweaver.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.BusinessLogConfig;
import mx.bastekor.flowweaver.config.InfoAppConfig;
import mx.bastekor.flowweaver.constant.FlowWeaverConstants;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static mx.bastekor.flowweaver.util.BusinessLogUtils.generateOperationCode;

@Slf4j
@Service
public class BusinessLogAspectService implements IBusinessLogAspectService {

    private final ExecutorService executorService;

    /**
     * Configuración de la sección "info.app" en el archivo de propiedades.
     */
    private final InfoAppConfig infoAppConfig;

    /**
     * Configuración tomada del archivo de propiedades en la sección
     * "flow-weaver.business-logs"
     */
    private final BusinessLogConfig businessLogConfig;


    public BusinessLogAspectService(BusinessLogConfig businessLogConfig, InfoAppConfig infoAppConfig) {
        this.businessLogConfig = businessLogConfig;
        this.infoAppConfig = infoAppConfig;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    @Override
    public void enqueue(BusinessLogEvent businessLogEvent) {
        final String flowWeaverContextId = businessLogEvent.getFlowWeaverContextId();
        executorService.submit(() -> {
            MDC.put(FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID, flowWeaverContextId);
            // Asignar/generar operationCode...
            businessLogEvent.getBusinessLogDTO().setOperationCode(generateOperationCode(businessLogEvent));
            try {
                // Aquí iría la lógica real: enviar a BD, Kafka, HTTP, etc.
                /*
                 * En este punto deberemos de inyectar el servicio que reciba los objetos a manipular en claro,
                 * con en claro me refiero a que se envíen objetos que no dependan de la configuración, proceso,
                 * etc, etc.
                 * El servicio podrá ser reutilizado por aquellos a los que no les guste programar con anotaciones,
                 * dejándoles que ellos realicen la implementación a mano, pero que esta implementación sea al final
                 * del día controlado por este componente.
                 */
                this.logData(businessLogEvent);
                // Simulación de trabajo real
                Thread.sleep(50);
            } catch (Exception e) {
                // Aquí puedes guardar en disco, en una cola local o reintentar
                log.error("[AUDIT][ERROR] Falló el procesamiento del evento: {}", e.getMessage());
            }
        });
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

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}