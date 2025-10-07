package mx.bastekor.flowweaver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.Argument;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.model.MethodContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.env.Environment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mx.bastekor.flowweaver.mapper.BusinessLogMapper.createBusinessLogDTO;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessLogUtils {

    /**
     * Método encargado de crear el DTO base de los datos interceptados. Esté DTO solo debería de
     * poder funcionar con la construcción de los datos enviados por el interceptor y no desde
     * cualquier otro lado en donde se este creando una nueva instancia.
     *
     * @param joinPoint   Interceptor del evento
     * @param businessLog Anotación interceptada
     * @param status      Enum con el valor del resultado (SUCCESS | FAILURE).
     * @param output      Valor del resultado del método interceptado (puede ser nulo si es que hubo excepción).
     * @param exception   Excepción interceptada (puede ser nulo si es que todo funciono bien).
     * @return Objeto {@link BusinessLogEvent} con los datos recuperados del interceptor.
     */
    public static BusinessLogEvent buildBusinessLogEvent(ProceedingJoinPoint joinPoint,
                                                         BusinessLog businessLog,
                                                         StatusEnum status,
                                                         Object output,
                                                         Throwable exception) {
        final BusinessLogEvent businessLogEvent = new BusinessLogEvent();
        businessLogEvent.setFlowWeaverContextId(FlowWeaverContextHolder.getFlowId());
        businessLogEvent.setDuration(FlowWeaverContextHolder.getDuration()); // Tiempo que tardo el proceso...
        businessLogEvent.setMethodContext(createMethodContext(joinPoint, output, exception)); // Contexto del método interceptado...
        businessLogEvent.setBusinessLogDTO(createBusinessLogDTO(businessLog)); // Transformación de la anotación a objeto
        businessLogEvent.setStatus(status); // Estatus que representa si termino correctamente o con error
        generateFlowCode(businessLogEvent); // Se valida el "flowCode" y se genera si es que no lo tiene.
        return businessLogEvent;
    }

    /**
     * Obtiene y establece en el RequestDTO el nombre del host y la dirección IP del equipo
     * donde se está ejecutando la aplicación.
     * En caso de no poder obtener la información, se registra una advertencia en el log.
     *
     * @param requestDTO DTO donde se asignarán los datos de infraestructura (hostName e ipAddress).
     */
    public static void getHostNameAndIpAddress(final RequestDTO requestDTO) {
        try {
            // Hostname
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            requestDTO.setHostName(hostName);

            // IP Address
            String ipAddress = java.net.InetAddress.getLocalHost().getHostAddress();
            requestDTO.setIpAddress(ipAddress);
        } catch (Exception e) {
            log.warn("No se pudo obtener información de infraestructura: {}", e.getMessage());
        }
    }

    /**
     * Obtiene el valor de una propiedad buscando secuencialmente por cada clave proporcionada.
     * Primero intenta obtenerla del Environment de Spring y, si useEnv es true, intenta también
     * desde variables de entorno del sistema. Si no encuentra ningún valor, devuelve el valor por defecto.
     *
     * @param keys          Arreglo de posibles claves a consultar (en orden de prioridad).
     * @param defaultValue  Valor por defecto a retornar si no se encuentra ninguna clave.
     * @param useEnv        Si es true, habilita la búsqueda en variables de entorno del sistema.
     * @param environment   Entorno de Spring para la búsqueda de propiedades.
     * @return El primer valor no vacío encontrado o defaultValue si no se encuentra ninguno.
     */
    private static String getPropertyValue(final String[] keys, final String defaultValue, boolean useEnv, 
                                          final Environment environment) {

        for (String key : keys) {
            String value = environment.getProperty(key);
            if (isNotBlank(value)) {
                return value;
            }
            if (useEnv) {
                value = System.getenv(key);
                if (isNotBlank(value)) {
                    return value;
                }
            }
        }
        return defaultValue;
    }

    /**
     * Completa en el {@link RequestDTO} la información básica de la aplicación que ejecuta el proceso.
     * Busca de manera secuencial el nombre, la versión y la descripción de la aplicación utilizando
     * distintas claves de configuración en el {@link Environment} de Spring (en ese orden de prioridad
     * por cada campo). Si no se encuentra un valor para alguna clave, el campo correspondiente se deja en null.
     *
     * Claves consideradas por cada campo, en orden de prioridad: </br>
     * - Nombre: info.app.name, spring.application.name, application.name, app.name </br>
     * - Versión: info.app.version, spring.application.version, application.version, app.version </br>
     * - Descripción: info.app.description, spring.application.description, application.description, app.description </br>
     *
     * Nota: Este método no consulta variables de entorno del sistema; únicamente propiedades del Environment de Spring.
     *
     * @param requestDTO   Objeto destino donde se establecerán nombre, versión y descripción de la app.
     * @param environment  Fuente de propiedades de Spring usada para resolver las claves configuradas.
     */
    public static void fillAppInfo(final RequestDTO requestDTO, final Environment environment) {
        String[] nameKeys = {
                "info.app.name",
                "spring.application.name",
                "application.name",
                "app.name"
        };
        String[] versionKeys = {
                "info.app.version",
                "spring.application.version",
                "application.version",
                "app.version"
        };
        String[] descKeys = {
                "info.app.description",
                "spring.application.description",
                "application.description",
                "app.description"
        };

        requestDTO.setAppName(getPropertyValue(nameKeys, null, false, environment));
        requestDTO.setAppVersion(getPropertyValue(versionKeys, null, false, environment));
        requestDTO.setAppDescription(getPropertyValue(descKeys, null, false, environment));
    }

    /**
         * Completa en el RequestDTO información de infraestructura de la ejecución (región, zona e id de instancia).
         * Busca los valores en propiedades de Spring y, si aplica, en variables de entorno comunes.
         *
         * Claves consideradas por cada campo, en orden de prioridad:
         * - region: cloud.region, CLOUD_REGION
         * - zone: cloud.zone, CLOUD_ZONE
         * - instanceId: cloud.instance.id, CLOUD_INSTANCE_ID
         *
         * @param requestDTO   DTO de solicitud donde se establecerán los datos de infraestructura.
         * @param environment  Environment de Spring usado para consultar propiedades de configuración.
         */
        public static void fillInfrastructureInfo(final RequestDTO requestDTO, final Environment environment) {

        String[] regionKeys = {
                "cloud.region",
                "CLOUD_REGION"
        };

        String[] zoneKeys = {
                "cloud.zone",
                "CLOUD_ZONE"
        };

        String[] instanceIdKeys = {
                "cloud.instance.id",
                "CLOUD_INSTANCE_ID"
        };

        requestDTO.setRegion(getPropertyValue(regionKeys, null, true, environment));
        requestDTO.setZone(getPropertyValue(zoneKeys, null, true, environment));
        requestDTO.setInstanceId(getPropertyValue(instanceIdKeys, null, true, environment));
    }

    /**
     * Método encargado de crear el objeto {@link MethodContext} con los valores del método interceptado por
     * el aspecto.
     *
     * @param joinPoint Punto de interceptor.
     * @param output    Salida del método, puede ser nula por error o por se un método void.
     * @param exception Excepción lanza en el método.
     * @return objeto {@link MethodContext}.
     */
    private static MethodContext createMethodContext(ProceedingJoinPoint joinPoint, Object output, Throwable exception) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return new MethodContext()
                .setClassName(method.getDeclaringClass().getSimpleName())
                .setMethodName(method.getName())
                .setReturnType(method.getReturnType().getSimpleName())
                .setMethodAnnotations(getMethodAnnotations(method))
                .setArguments(getArguments(joinPoint))
                .setOutput(output)
                .setException(exception);
    }

    /**
     * Método encargado de validar si el "flowCode" existe y si no lo tiene, se genera uno por defecto.
     *
     * @param businessLogEvent Objeto llenado a partir del interceptor {@link BusinessLog}
     */
    private static void generateFlowCode(BusinessLogEvent businessLogEvent) {
        if (isBlank(businessLogEvent.getBusinessLogDTO().getFlowCode())) {
            final String flowCode = businessLogEvent.getMethodContext().getClassName() +
                    "#" +
                    businessLogEvent.getMethodContext().getMethodName();
            businessLogEvent.getBusinessLogDTO().setFlowCode(flowCode);
        }
    }

    /**
     * Método encargado de obtener la lista de los argumentos de la firma del método con la totalidad
     * de la representación y tratarlos en un objeto custom {@link BusinessLogEvent}.
     *
     * @param joinPoint Interceptor
     * @return Lista de objetos {@link Argument} con los metadatos de cada argumento.
     */
    private static List<Argument> getArguments(ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        List<Argument> arguments = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {

            Parameter parameter = parameters[i];
            String name = parameter.getName();
            String type = parameter.getType().getSimpleName();
            Object value = args[i];
            List<String> annotations = getArgumentAnnotations(parameterAnnotations, i);

            arguments.add(new Argument(i, name, type, value, annotations));
        }
        return arguments;
    }

    /**
     * Método encargado de obtener las anotaciones del método interceptado (si es que cuenta con ellas).
     *
     * @param method Interfaz de reflection
     * @return Lista de los nombres de las anotaciones (si es que cuenta con ellas).
     */
    private static List<String> getMethodAnnotations(Method method) {
        return Arrays.stream(method.getAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getSimpleName)
                .toList();
    }

    /**
     * Método encargado de obtener las anotaciones por cada uno de los argumentos del método
     * (si es que cuenta con ellas).
     *
     * @param parameterAnnotations Matriz de anotaciones
     * @param index                Indice del argumento
     * @return Lista de los nombres de las anotaciones (si es que cuenta con ellas).
     */
    private static List<String> getArgumentAnnotations(Annotation[][] parameterAnnotations, int index) {
        return Arrays.stream(parameterAnnotations[index])
                .map(Annotation::annotationType)
                .map(Class::getSimpleName)
                .toList();
    }
}