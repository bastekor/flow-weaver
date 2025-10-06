package mx.bastekor.flowweaver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.context.FlowWeaverContextHolder;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.Argument;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.model.MethodContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mx.bastekor.flowweaver.mapper.BusinessLogMapper.createBusinessLogDTO;
import static org.apache.commons.lang3.StringUtils.isBlank;

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