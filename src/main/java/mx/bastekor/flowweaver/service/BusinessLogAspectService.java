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
import static mx.bastekor.flowweaver.util.BusinessLogUtils.fillAppInfo;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.fillInfrastructureInfo;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.getHostNameAndIpAddress;

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
        final RequestDTO requestDTO = this.createRequestDTO(businessLogEvent);
    }

    
    /**
     * Crea y prepara un objeto RequestDTO a partir de la información contenida en un BusinessLogEvent.
     * Este método mapea los metadatos del flujo (id del contexto, flowCode, status y mode) y enriquece
     * la petición con información de la aplicación y de la infraestructura, obtenida del Environment
     * actual. Además, resuelve el nombre del host y la dirección IP de la máquina donde se ejecuta el servicio.
     * </br>
     * Detalles: </br>
     * - fillAppInfo: agrega datos de la aplicación (nombre, versión, perfiles activos, etc.). </br>
     * - fillInfrastructureInfo: agrega datos de infraestructura (entorno, zona, región, etc., según configuración). </br>
     * - getHostNameAndIpAddress: determina y asigna hostname e IP del servidor. </br>
     *
     * Nota: La descripción del evento aún no se establece porque falta la lógica para resolver valores no
     * predeterminados; ver los comentarios dentro del método para más contexto.
     *
     * @param businessLogEvent evento de negocio desde el cual se construye la solicitud.
     * @return RequestDTO construido y enriquecido, listo para su envío/serialización.
     */
    private RequestDTO createRequestDTO(final BusinessLogEvent businessLogEvent) {
        final RequestDTO requestDTO = RequestDTO.builder()
                .id(businessLogEvent.getFlowWeaverContextId())
                .flowCode(businessLogEvent.getBusinessLogDTO().getFlowCode())
                .status(businessLogEvent.getStatus().name())
                .mode(businessLogEvent.getBusinessLogDTO().getMode().name())
                .build();
        fillAppInfo(requestDTO, environment);
        fillInfrastructureInfo(requestDTO, environment);
        getHostNameAndIpAddress(requestDTO);

//        final BusinessLogDTO businessLogDTO = businessLogEvent.getBusinessLogDTO();
        // Esto aún no se puede porque falta la logica para obtener la data segun lo que no es default...
//        final String description = getPropertyValue(businessLogDTO.getDescription(), businessLogDTO.getDefaultDescription());
//        requestDTO.setDescription(description);
        return requestDTO;
    }
}