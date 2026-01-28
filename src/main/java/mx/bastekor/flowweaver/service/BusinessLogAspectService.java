package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.BusinessLogConfig;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_ERROR;
import static mx.bastekor.flowweaver.mapper.AuditTrailMapper.createAuditTrailDTO;
import static mx.bastekor.flowweaver.mapper.BusinessLogMapper.createBusinessLogDTO;
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
    public void processBusinessLog(final BusinessLogContainer businessLogContainer) {
        // Dejar siempre al principio marcando el fin del proceso del método anotado con @BusinessLog.
        final String methodDuration = businessLogContainer.getDuration(); // Tiempo que tomo tomar data "snapshot"

        final Instant start = Instant.now(); // Inicio de lógica de negocio.

        final String flowWeaverContextId = businessLogContainer.getOperationId();

        final BusinessLogDTO businessLogDTO = createBusinessLogDTO(businessLogContainer.getBusinessLog());
        // Se agrega operationCode en caso de que haya sido vacío desde @BusinessLog
        businessLogDTO.setOperationCode(businessLogContainer.getOperationCode());

        final RequestDTO requestDTO = RequestDTO.builder()
                .id(flowWeaverContextId)
                .flowCode(businessLogDTO.getOperationCode())
                .status(businessLogContainer.getStatus().name())
                .mode(businessLogDTO.getMode().name())
//                .data(new DataDTO()) // esto son valores reales finales
                .build();

        fillAppInfo(requestDTO, environment);
        fillInfrastructureInfo(requestDTO, environment);
        getHostNameAndIpAddress(requestDTO);

        // temporal para pruebas
        requestDTO.setResult(businessLogContainer.getExitSignature());
        final Instant end = Instant.now();

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
            log.error(BUSINESS_ERROR, businessLogContainer.getStatus(), flowWeaverContextId, e.getMessage(), e);
        }
    }

    @Override
    @Async("flowWeaverExecutor")
    public void processAuditTrail(final AuditTrailContainer auditTrailContainer) {
        // Dejar siempre al principio marcando el fin del proceso del método anotado con @AuditTrail.
        final String methodDuration = auditTrailContainer.getDuration();
        final Instant start = Instant.now();
        final AuditTrailDTO auditTrailDTO = createAuditTrailDTO(auditTrailContainer.getAuditTrail());
//        final MethodContext methodContext = createMethodContext(joinPoint, null, null);
        final Instant end = Instant.now();
    }
}