package mx.bastekor.flowweaver.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.dto.MethodSnapshotDTO;
import mx.bastekor.flowweaver.resolver.ExpressionResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static mx.bastekor.flowweaver.util.SafeSerializer.rawValue;
import static mx.bastekor.flowweaver.util.SafeSerializer.safeValue;
import static mx.bastekor.flowweaver.resolver.ExpressionResolver.resolve;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class SafeSnapshotMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
            Object raw = rawValue(value, 0, maxDepth);
            if (raw instanceof Map) {
                Object safe = safeValue(value, 0, maxDepth);
                if (safe instanceof Map) {
                    Object toString = ((Map<String, Object>) safe).get("_toString");
                    if (toString != null) {
                        ((Map<String, Object>) raw).put("_toString", toString);
                    }
                }
            }
            root.put(prefix, raw);
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize object: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"Unexpected error: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Operación inversa de {@link #mapArgs(ProceedingJoinPoint, int)}: reconstruye
     * los argumentos del método desde el snapshot JSON de la firma.
     * <p>
     * Devuelve un mapa {@code nombreLiteral -> valor} con los tipos naturales de
     * los datos (String, número, booleano, lista, mapa). Los valores se toman del
     * nodo {@code _fields}, excluyendo las claves {@code argsN}. {@code null}-safe.
     *
     * @param mapArgsJson snapshot JSON generado por {@code mapArgs}.
     * @return mapa nombre -> valor, o {@code null} si no hay argumentos o el JSON no es válido.
     */
    public static Map<String, Object> unmapArgs(final String mapArgsJson) {
        if (mapArgsJson == null) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(mapArgsJson);
            JsonNode fields = root == null ? null : root.get("_fields");
            if (fields == null || !fields.isObject()) {
                return null;
            }
            Map<String, Object> args = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = fields.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                if (entry.getKey().matches("args\\d+")) {
                    continue;
                }
                args.put(entry.getKey(), MAPPER.treeToValue(entry.getValue(), Object.class));
            }
            return args.isEmpty() ? null : args;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Operación inversa de {@link #mapObject(Object, int)}: reconstruye el objeto de
     * respuesta tipado desde el snapshot JSON de salida.
     * <p>
     * Requiere {@code responseType} porque el snapshot de salida no conserva el
     * FQCN del objeto (solo {@code mapArgs} guarda {@code _returnType}). Devuelve
     * {@code null} si el nodo {@code response} no existe (por ejemplo cuando se
     * serializó una excepción), el tipo es {@code null} o la conversión falla.
     *
     * @param mapObjectJson snapshot JSON generado por {@code mapObject}.
     * @param responseType  clase objetivo del objeto de respuesta.
     * @return POJO reconstruido, o {@code null}.
     */
    public static <T> T unmapObject(final String mapObjectJson, final Class<T> responseType) {
        if (mapObjectJson == null || responseType == null) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(mapObjectJson);
            JsonNode response = root == null ? null : root.get("response");
            if (response == null || response.isNull() || response.isMissingNode()) {
                return null;
            }
            return MAPPER.treeToValue(response, responseType);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Construye la firma del método (POJO) a partir de los snapshots JSON de entrada
     * y salida, haciendo la "deconstrucción" inversa a {@code mapArgs}/{@code mapObject}
     * y reutilizando {@link ExpressionResolver#resolve(String, String, String)} para
     * los campos escalares ({@code _type}, {@code _method}, {@code _returnType}).
     * {@code null}-safe: campos no reconstruibles quedan en {@code null} sin lanzar.
     *
     * @param mapArgsJson    snapshot JSON de la firma del método.
     * @param mapObjectJson  snapshot JSON del resultado (response o exception).
     * @param methodDuration duración del método anotado.
     */
    @SuppressWarnings("unchecked")
    public static <T> MethodSnapshotDTO<T> toMethodSnapshot(final String mapArgsJson, final String mapObjectJson,
                                                            final String methodDuration) {
        final MethodSnapshotDTO<T> signature = new MethodSnapshotDTO<>();
        signature.setClassName(resolve(mapArgsJson, null, "_type"));
        signature.setMethodName(resolve(mapArgsJson, null, "_method"));
        signature.setReturnType(resolve(mapArgsJson, null, "_returnType"));
        signature.setMethodDuration(methodDuration);
        signature.setArgs(unmapArgs(mapArgsJson));
        final boolean exception = isException(mapObjectJson);
        signature.setException(exception);
        signature.setResponse(exception ? null : unmapObject(mapObjectJson, (Class<T>) resolveResponseType(mapArgsJson)));
        return signature;
    }

    private static boolean isException(final String mapObjectJson) {
        if (mapObjectJson == null) {
            return false;
        }
        try {
            JsonNode root = MAPPER.readTree(mapObjectJson);
            return root != null && root.isObject() && root.has("exception");
        } catch (Exception e) {
            return false;
        }
    }

    private static Class<?> resolveResponseType(final String mapArgsJson) {
        final String returnType = resolve(mapArgsJson, null, "_returnType");
        if (returnType == null || returnType.isBlank() || "void".equalsIgnoreCase(returnType)) {
            return null;
        }
        try {
            return Class.forName(returnType);
        } catch (ClassNotFoundException e) {
            return null;
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
            if (raw instanceof Map && safeResult instanceof Map) {
                Object safeToString = ((Map<String, Object>) safeResult).get("_toString");
                if (safeToString != null) {
                    ((Map<String, Object>) raw).put("_toString", safeToString);
                }
            }
            fields.put(prefix, raw);
            fields.put(parameter.getName(), raw);
        }
        root.put("_args", argsList);
        root.put("_fields", fields);
        return MAPPER.writeValueAsString(root);
    }
}
