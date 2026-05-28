package mx.bastekor.flowweaver.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static mx.bastekor.flowweaver.model.SafeSerializer.rawValue;
import static mx.bastekor.flowweaver.model.SafeSerializer.safeValue;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class SafeSnapshotMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Método encargado de generar en formato JSON la firma completa del método anotado.
     *
     * @param joinPoint Objeto interceptor
     * @param maxDepth  máximo nivel de anidamiento
     * @return Cadena en formato JSON
     */
    public static String mapArgs(ProceedingJoinPoint joinPoint, int maxDepth) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            return mapArgs(method, joinPoint.getArgs(), maxDepth);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize snapshot: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"Unexpected error: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Serializa un objeto (response, exception, etc.) a JSON con un prefijo semántico.
     * <p>
     * Si el objeto es una instancia de {@link Throwable} el prefijo será
     * {@code "exception"}; en cualquier otro caso será {@code "response"}.
     * <p>
     * El JSON generado puede consultarse directamente con {@link
     * mx.bastekor.flowweaver.resolver.ExpressionResolver}:
     * <pre>
     *   ExpressionResolver.resolve(mapObject(persona, 3), "response.name")
     * </pre>
     *
     * @param value    objeto a serializar
     * @param maxDepth profundidad máxima de anidamiento
     * @return JSON con un nodo raíz {@code "response"} o {@code "exception"}
     */
    public static String mapObject(Object value, int maxDepth) {
        try {
            String prefix = (value instanceof Throwable) ? "exception" : "response";
            Map<String, Object> root = new LinkedHashMap<>();
            root.put(prefix, rawValue(value, 0, maxDepth));
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize object: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"Unexpected error: " + e.getMessage() + "\"}";
        }
    }

    private static String mapArgs(Method method, Object[] args, int maxDepth) throws Exception {
        Parameter[] parameters = method.getParameters();

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("_type", method.getDeclaringClass().getName());
        root.put("_method", method.getName());
        root.put("_returnType", method.getReturnType().getName());

        List<String> annotationsList = new ArrayList<>();
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof BusinessLog || annotation instanceof AuditTrail) {
                annotationsList.add(annotation.toString());
            } else {
                annotationsList.add("@" + annotation.annotationType().getSimpleName());
            }
        }
        root.put("_annotations", annotationsList);

        List<Object> argsList = new ArrayList<>();
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object safeResult = safeValue(args[i], 0, maxDepth);

            Map<String, Object> argMap = new LinkedHashMap<>();
            argMap.put("_index", i);
            argMap.put("_name", parameter.getName());

            if (safeResult instanceof Map) {
                Map<String, Object> safeMap = (Map<String, Object>) safeResult;
                argMap.put("_type", safeMap.get("_type"));
                argMap.put("_toString", safeMap.get("_toString"));
                argMap.put("_value", safeMap.get("_value"));
            } else {
                argMap.put("_type", parameter.getType().getName());
                argMap.put("_toString", safeResult);
                argMap.put("_value", safeResult);
            }

            argsList.add(argMap);
            String prefix = "args" + i;
            Object raw = rawValue(args[i], 0, maxDepth);
            root.put(prefix, raw);
            fields.put("args" + i, raw);
            fields.put(parameter.getName(), raw);
        }
        root.put("_args", argsList);
        root.put("_fields", fields);
        return MAPPER.writeValueAsString(root);
    }
}
