package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.BusinessLogConfig;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessLogAspectService implements IBusinessLogAspectService {

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
        // Armar DTO a mandar para trazabilidad.
        RequestDTO requestDTO = RequestDTO.builder()
                .id(businessLogEvent.getFlowWeaverContextId())
                .flowCode(businessLogEvent.getBusinessLogDTO().getFlowCode())
                .status(businessLogEvent.getStatus().name())
                .mode(businessLogEvent.getBusinessLogDTO().getMode().name())
                .build();

        fillAppInfo(requestDTO);
        fillInfrastructureInfo(requestDTO);

        System.out.printf("Resultado cachado con @BusinessLog:: %s%n", requestDTO);

        // Extraer datos según lógica implementada para dejar llave/valor


        // Se manda DtoRequest a la cola de trazabilidad y se espera la respuesta DtoResponse.
        // Se procesa el DtoResponse quien contendrá la lógica de trazabilidad que se envío/logueo.
//        this.logData(businessLogEvent);
    }

    /**
     * Obtiene información relevante de la aplicación y la asigna al RequestDTO.
     * Busca las propiedades en diferentes ubicaciones posibles.
     */
    private void fillAppInfo(final RequestDTO requestDTO) {
        String[] nameKeys = {
                "info.app.name", "spring.application.name", "application.name", "app.name"
        };
        String[] versionKeys = {
                "info.app.version", "spring.application.version", "application.version", "app.version"
        };
        String[] descKeys = {
                "info.app.description", "spring.application.description", "application.description", "app.description"
        };

        for (String key : nameKeys) {
            String value = environment.getProperty(key);
            if (isNotBlank(value)) {
                requestDTO.setAppName(value);
                break;
            }
        }
        for (String key : versionKeys) {
            String value = environment.getProperty(key);
            if (isNotBlank(value)) {
                requestDTO.setAppVersion(value);
                break;
            }
        }
        for (String key : descKeys) {
            String value = environment.getProperty(key);
            if (isNotBlank(value)) {
                requestDTO.setDescription(value);
                break;
            }
        }
    }

    private void fillInfrastructureInfo(final RequestDTO requestDTO) {

        try {
            // Hostname
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            requestDTO.setHostName(hostName);

            // IP Address
            String ipAddress = java.net.InetAddress.getLocalHost().getHostAddress();
            requestDTO.setIpAddress(ipAddress);

            // Region (try to get from environment or system properties)
            String region = environment.getProperty("cloud.region");
            if (isBlank(region)) {
                region = System.getenv("CLOUD_REGION");
            }
            requestDTO.setRegion(region);

            // Zone (try to get from environment or system properties)
            String zone = environment.getProperty("cloud.zone");
            if (isBlank(zone)) {
                zone = System.getenv("CLOUD_ZONE");
            }
            requestDTO.setZone(zone);

            // Instance ID (try to get from environment or system properties)
            String instanceId = environment.getProperty("cloud.instance.id");
            if (isBlank(instanceId)) {
                instanceId = System.getenv("CLOUD_INSTANCE_ID");
            }
            requestDTO.setInstanceId(instanceId);
        } catch (Exception e) {
            log.warn("No se pudo obtener información de infraestructura: {}", e.getMessage());
        }
    }


//    private void logData(BusinessLogEvent businessLogEvent) {
//        log.info("[AUDIT] Información de la Aplicación: {}", infoAppConfig);
//        log.info("[AUDIT] Entrando evento: {}", businessLogEvent);
//        log.info("[AUDIT] Configuraciones: {}", businessLogConfig);
//
//        /*
//         * Aquí deberíamos de procesar todos los datos y transformarlos en objetos planos reutilizables
//         * fuera de la lógia prevía en donde se obtuvieron los datos.
//         */
//    }
}