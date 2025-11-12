package mx.bastekor.flowweaver.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashMap;
import java.util.Map;

import static mx.bastekor.flowweaver.model.SafeSerializer.safeValue;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.getArgumentAnnotations;
import static mx.bastekor.flowweaver.util.BusinessLogUtils.getMethodAnnotations;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class SafeSnapshotMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String mapArgs(ProceedingJoinPoint joinPoint, int maxDepth) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("_class", signature.getDeclaringTypeName());
            root.put("_method", method.getName());
            root.put("_returnType", method.getReturnType().getSimpleName());
            root.put("_annotations", getMethodAnnotations(method));

            Map<String, Object> argsMap = new LinkedHashMap<>();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Map<String, Object> object = new LinkedHashMap<>();
                object.put("index", i);
                object.put("name", parameter.getName());
                object.putAll(safeValue(args[i], 0, maxDepth));
                object.put("_annotations", getArgumentAnnotations(method.getParameterAnnotations(), i));
                argsMap.put("arg[" + i + "]", object);
            }
            root.put("args", argsMap);
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"Failed to serialize snapshot: " + e.getMessage() + "\"}";
        } catch (Exception e) {
            return "{\"error\":\"Unexpected error: " + e.getMessage() + "\"}";
        }
    }
}
