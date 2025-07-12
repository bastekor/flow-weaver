package mx.bastekor.flowweaver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.mapper.BusinessLogMapper;
import mx.bastekor.flowweaver.model.BusinessLogDTO;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.model.DataParamDTO;
import mx.bastekor.flowweaver.model.MethodArg;
import mx.bastekor.flowweaver.model.MethodContext;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessLogUtils {

    /**
     * Método encargado de crear el DTO base de los datos interceptados. Esté DTO solo debería de
     * poder funcionar con la construcción de los datos enviados por el interceptor y no desde
     * cualquier otro lado en donde se este creando una nueva instancia.
     *
     * @param joinPoint   Interceptor del evento
     * @param businessLog Anotación interceptada
     * @param start       Objeto Instant que representa el inicio del proceso.
     * @param end         Objeto Instant que representa el fin del proceso.
     * @param status      Enum con el valor del resultado (SUCCESS | FAILURE).
     * @param output      Valor del resultado del método interceptado (puede ser nulo si es que hubo excepción).
     * @param exception   Excepción interceptada (puede ser nulo si es que todo funciono bien).
     * @return Objeto {@link BusinessLogEvent} con los datos recuperados del interceptor.
     */
    public static BusinessLogEvent buildBusinessLogEvent(ProceedingJoinPoint joinPoint,
                                                         BusinessLog businessLog,
                                                         Instant start,
                                                         Instant end,
                                                         StatusEnum status,
                                                         Object output,
                                                         Throwable exception) {
        return new BusinessLogEvent()
                .setFlowWeaverContextId(FlowWeaverContextHolder.get().getFlowId())
                .setDuration(calculateDuration(start, end)) // Tiempo que tardo el proceso...
                .setMethodContext(createMethodContext(joinPoint, output, exception)) // Contexto del método interceptado...
                .setBusinessLogDTO(BusinessLogMapper.INSTANCE.createBusinessLogDTO(businessLog)) // Transformación de la anotación a objeto
                .setStatus(status); // Estatus que representa si termino correctamente o con error
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
                .setArguments(getMethodArgs(joinPoint))
                .setOutput(output)
                .setException(exception);
    }

    /**
     * Método encargado de recuperar/generar el código de la operación del flujo.
     *
     * @param businessLogEvent Objeto llenado a partir del interceptor {@link BusinessLog}
     * @return Código de la operación.
     */
    public static String generateOperationCode(BusinessLogEvent businessLogEvent) {
        return Optional.of(businessLogEvent)
                .map(BusinessLogEvent::getBusinessLogDTO)
                .map(BusinessLogDTO::getOperationCode)
                .filter(StringUtils::isNotBlank)
                .orElse(businessLogEvent.getMethodContext().getClassName() +
                        "#" +
                        businessLogEvent.getMethodContext().getMethodName());
    }

    /**
     * Método encargado de obtener la lista de los argumentos de la firma del método con la totalidad
     * de la representación y tratarlos en un objeto custom {@link BusinessLogEvent}.
     *
     * @param joinPoint Interceptor
     * @return Lista de objetos {@link MethodArg} con los metadatos de cada argumento.
     */
    private static List<MethodArg> getMethodArgs(ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        List<MethodArg> methodArgs = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {

            Parameter parameter = parameters[i];
            String name = parameter.getName();
            String type = parameter.getType().getSimpleName();
            Object value = args[i];
            List<String> annotations = getArgumentAnnotations(parameterAnnotations, i);

            methodArgs.add(new MethodArg(i, name, type, value, annotations));
        }
        return methodArgs;
    }

    /**
     * Método encargado de obtener las anotaciones del método interceptado (si es que cuenta con ellas).
     *
     * @param method Interfaz de reflection
     * @return Lista de los nombres de las anotaciones (si es que cuenta con ellas).
     */
    private static List<String> getMethodAnnotations(Method method) {
        return Arrays.stream(method.getAnnotations())
//                .map(annotation -> annotation.annotationType().getSimpleName())
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
//                .map(annotations -> annotations.annotationType().getSimpleName())
                .map(Annotation::annotationType)
                .map(Class::getSimpleName)
                .toList();
    }

    /**
     * Método encargado de realizar la estimación de la duración del proceso interceptado, retornando
     * el valor en una cadena de texto similar a los siguientes resultados: ["1s 245ms" o "135ms"]
     *
     * @param start Objeto Instant de inicio del proceso.
     * @param end   Objeto Instant de fin del proceso.
     * @return Duración calculada del proceso.
     */
    private static String calculateDuration(Instant start, Instant end) {
        long millis = Duration.between(start, end).toMillis();

        long seconds = millis / 1000;
        long remainderMillis = millis % 1000;

        if (seconds > 0) {
            return String.format("%ds %dms", seconds, remainderMillis);
        } else {
            return String.format("%dms", remainderMillis);
        }
    }
}