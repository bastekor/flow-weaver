package mx.bastekor.flowweaver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.model.Argument;
import mx.bastekor.flowweaver.model.MethodContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BusinessLogUtils {

    /**
     * Método encargado de crear el objeto {@link MethodContext} con los valores del método interceptado por
     * el aspecto.
     *
     * @param joinPoint Punto de interceptor.
     * @param output    Salida del método, puede ser nula por error o por se un método void.
     * @param exception Excepción lanza en el método.
     * @return objeto {@link MethodContext}.
     */
    public static MethodContext createMethodContext(ProceedingJoinPoint joinPoint, Object output, Throwable exception) {
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
     * Método encargado de obtener la lista de los argumentos de la firma del método con la totalidad
     * de la representación y tratarlos en un objeto custom.
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
    public static List<String> getMethodAnnotations(Method method) {
        return Arrays.stream(method.getAnnotations())
                .map(Annotation::annotationType)
                .map(Class::getSimpleName)
                .map(annotation -> "@" + annotation)
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
    public static List<String> getArgumentAnnotations(Annotation[][] parameterAnnotations, int index) {
        return Arrays.stream(parameterAnnotations[index])
                .map(Annotation::annotationType)
                .map(Class::getSimpleName)
                .map(annotation -> "@" + annotation)
                .toList();
    }
}