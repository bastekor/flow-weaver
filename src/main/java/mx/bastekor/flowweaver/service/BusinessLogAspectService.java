package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.BusinessLogConfig;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_ERROR;
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
    public void processBusinessLog(final BusinessLogEvent businessLogEvent, final StatusEnum status, final String flowId) {
        MDC.put(FLOW_WEAVER_CONTEXT_ID, flowId);
        final RequestDTO requestDTO = this.createRequestDTO(businessLogEvent);
        try {
            // Aquí se invoca la lógica para recuperar data dinámicamente, si algo falla (lógica de negocio o lógica de programación)
            // almacenar el tipo de error provocado, además de los pocos datos que se lograrón recuperar hasta el momento.

            // Cabe mencionar que deberemos de generar algún objeto mutable el cual en los diferentes flujos se vaya actualizando
            // con los datos que se recuperen. Con esto aseguramos que se están extrayendo la mayor cantidad de datos posibles y
            // que sin importar en donde falle, se obtuvieron la mayoría posible.

            /*
            Ejemplo: Supongamos que debemos de entregar dentro de la lógica la extracción de todos los datos estáticos,
            dinámicos y recuperados de donde sea, entonces:
            1. Falla por lógica de negocio:
                Cuando no logremos recuperar datos en alguno de los modos (STATIC, DYNAMIC, MERGED), etc.
            2. Falla por lógica de programación:
                Cuando se mande una excepción (NullPointerException, IndexOutOfBoundsException, etc.) que no sea controlada
                por nosotros y que se entienda se esté estimando mal la extracción de la data.
             */
            log.info("Request: {}", requestDTO);
        } catch (Exception e) {
            log.error(BUSINESS_LOG_ERROR, status, flowId, e.getMessage(), e);
        }
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
     * <p>
     * Nota: La descripción del evento aún no se establece porque falta la lógica para resolver valores no
     * predeterminados; ver los comentarios dentro del método para más contexto.
     *
     * @param businessLogEvent evento de negocio desde el cual se construye la solicitud.
     * @return RequestDTO construido y enriquecido, listo para su envío/serialización.
     */
    private RequestDTO createRequestDTO(final BusinessLogEvent businessLogEvent) {
        final RequestDTO requestDTO = RequestDTO.builder()
                .id(businessLogEvent.getFlowWeaverContextId())
                .flowCode(businessLogEvent.getBusinessLogDTO().getOperationCode())
                .status(businessLogEvent.getStatus().name())
                .mode(businessLogEvent.getBusinessLogDTO().getMode().name())
                .build();
        fillAppInfo(requestDTO, environment);
        fillInfrastructureInfo(requestDTO, environment);
        getHostNameAndIpAddress(requestDTO);

//        final BusinessLogDTO businessLogDTO = businessLogEvent.getBusinessLogDTO();
        // Esto aún no se puede porque falta la lógica para obtener la data según lo que no es default...
//        final String description = getPropertyValue(businessLogDTO.getDescription(), businessLogDTO.getDefaultDescription());
//        requestDTO.setDescription(description);
        return requestDTO;
    }
}