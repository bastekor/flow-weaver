package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.BusinessLogConfig;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.mapper.UtilMapper;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_ERROR;
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

    private final UtilMapper utilMapper;

    @Override
    @Async("flowWeaverExecutor")
    public void processBusinessLog(final BusinessLogContainer businessLogContainer) {
        // Dejar siempre al principio marcando el fin del proceso del método anotado con @BusinessLog.
        final String methodDuration = businessLogContainer.getDuration(); // Tiempo que tomo tomar data "snapshot"
        final Instant start = Instant.now(); // Inicio de lógica de negocio.
        final BusinessLogDTO businessLogDTO = this.getBusinessLogDTO(businessLogContainer);

        final RequestDTO requestDTO = RequestDTO.builder()
                .id(businessLogContainer.getOperationId())
                .groupCode(businessLogDTO.getGroupCode())
                .flowCode(businessLogDTO.getOperationCode())
                .status(businessLogContainer.getStatus().name())
                .mode(businessLogDTO.getMode().name())
//                .data(new DataDTO()) // esto son valores reales finales del mapa data-out
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
            Map<String, BusinessLogDTO> businessLogs = businessLogConfig.getBusinessLogs();


            log.info("Request-BusinessLog: {}", requestDTO);
            log.info("Config :: {}", businessLogConfig.getBusinessLogs());
        } catch (Exception e) {
            log.error(BUSINESS_ERROR, businessLogContainer.getStatus(), businessLogContainer.getOperationCode(), e.getMessage(), e);
        }
    }

    @Override
    @Async("flowWeaverExecutor")
    public void processAuditTrail(final AuditTrailContainer auditTrailContainer) {
        // Dejar siempre al principio marcando el fin del proceso del método anotado con @AuditTrail.
        final String methodDuration = auditTrailContainer.getDuration();
        final Instant start = Instant.now();
        final AuditTrailDTO auditTrailDTO = this.getAuditTrailDTO(auditTrailContainer);
//        final MethodContext methodContext = createMethodContext(joinPoint, null, null);
        final String status = auditTrailContainer.getStatus() == null ? null : auditTrailContainer.getStatus().name();
        final RequestDTO requestDTO = RequestDTO.builder()
                .id(auditTrailContainer.getFlowId())
                .flowCode(auditTrailDTO.getFlowCode())
                .status(status)
                .mode(auditTrailDTO.getMode().name())
//                .data(new DataDTO()) // esto son valores reales finales
                .build();

        fillAppInfo(requestDTO, environment);
        fillInfrastructureInfo(requestDTO, environment);
        getHostNameAndIpAddress(requestDTO);

        // temporal para pruebas
        final String result = (auditTrailContainer.getEntrySignature() == null) ?
                auditTrailContainer.getExitSignature() : auditTrailContainer.getEntrySignature();
        requestDTO.setResult(result);
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
            log.info("Request-AuditTrail: {}", requestDTO);
            log.info("Config :: {}", businessLogConfig.getAuditTrails());
        } catch (Exception e) {
            log.error(AUDIT_ERROR, auditTrailContainer.getStatus(), auditTrailContainer.getFlowId(), e.getMessage(), e);
        }
    }


    /**
     * Método encargado de obtener el objeto DTO de la anotación @BusinessLog siguiendo el modo de obtención
     * pasado (STATIC, DYNAMIC, MERGED) en la misma anotación.
     * <p>
     * Por el momento no se contemplan fix en runtime en caso de que se equivoque la config y/o no venir desde
     * variables de configuración
     *
     * @param businessLogContainer Objeto de negocio.
     * @return @{@link BusinessLogDTO}
     */
    private BusinessLogDTO getBusinessLogDTO(final BusinessLogContainer businessLogContainer) {

        BusinessLogDTO blStatic = createBusinessLogDTO(businessLogContainer);
        BusinessLogDTO blDynamic = Optional.ofNullable(businessLogConfig.getBusinessLogs())
                .map(bl -> bl.get(businessLogContainer.getOperationCode()))
                .orElse(null);

        return switch (businessLogContainer.getBusinessLog().mode()) {
            case STATIC -> blStatic;
            case DYNAMIC -> blDynamic;
            case MERGED -> utilMapper.mergeBusinessLogDTO(blDynamic, blStatic);
        };
    }

    /**
     * Método encargado de obtener el objeto DTO de la anotación @AuditTrail siguiendo el modo de obtención
     * pasado (STATIC, DYNAMIC, MERGED) en la misma anotación.
     * <p>
     * Por el momento no se contemplan fix en runtime en caso de que se equivoque la config y/o no venir desde
     * variables de configuración
     *
     * @param auditTrailContainer Objeto de negocio.
     * @return @{@link AuditTrailDTO}
     */
    private AuditTrailDTO getAuditTrailDTO(final AuditTrailContainer auditTrailContainer) {
        AuditTrailDTO atStatic = createAuditTrailDTO(auditTrailContainer);
        AuditTrailDTO atDynamic = Optional.ofNullable(businessLogConfig.getAuditTrails())
                .map(at -> at.get(auditTrailContainer.getOperationCode()))
                .orElse(null);

        return switch (auditTrailContainer.getAuditTrail().mode()) {
            case STATIC -> atStatic;
            case DYNAMIC -> atDynamic;
            case MERGED -> utilMapper.mergeAuditTrailDTO(atDynamic, atStatic);
        };
    }
}