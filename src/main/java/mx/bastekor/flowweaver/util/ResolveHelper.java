package mx.bastekor.flowweaver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.dto.SimpleRequestDTO;
import mx.bastekor.flowweaver.resolver.ExpressionResolver;
import mx.bastekor.flowweaver.resolver.ResolutionResult;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.of;
import static mx.bastekor.flowweaver.util.Util.normalizeInput;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolveHelper {

    public static void getHostNameAndIpAddress(final SimpleRequestDTO requestDTO) {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            requestDTO.setHostName(hostName);
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            requestDTO.setIpAddress(ipAddress);
        } catch (Exception e) {
            log.warn("No se pudo obtener información de infraestructura: {}", e.getMessage());
        }
    }

    private static String getPropertyValue(final String[] keys, final String defaultValue,
                                           final boolean useEnv, final Environment environment) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (isNotBlank(value)) return value;
            if (useEnv) {
                value = System.getenv(key);
                if (isNotBlank(value)) return value;
            }
        }
        return defaultValue;
    }

    public static void fillAppInfo(final SimpleRequestDTO requestDTO, final Environment environment) {
        requestDTO.setAppName(getPropertyValue(
                new String[]{"info.app.name", "spring.application.name", "application.name", "app.name"},
                null, false, environment));
        requestDTO.setAppVersion(getPropertyValue(
                new String[]{"info.app.version", "spring.application.version", "application.version", "app.version"},
                null, false, environment));
        requestDTO.setAppDescription(getPropertyValue(
                new String[]{"info.app.description", "spring.application.description", "application.description", "app.description"},
                null, false, environment));
    }

    public static void fillInfrastructureInfo(final SimpleRequestDTO requestDTO, final Environment environment) {
        requestDTO.setRegion(getPropertyValue(
                new String[]{"cloud.region", "CLOUD_REGION"}, null, true, environment));
        requestDTO.setZone(getPropertyValue(
                new String[]{"cloud.zone", "CLOUD_ZONE"}, null, true, environment));
        requestDTO.setInstanceId(getPropertyValue(
                new String[]{"cloud.instance.id", "CLOUD_INSTANCE_ID"}, null, true, environment));
    }

    public static String resolveResult(final String jsonReq,
                                       final String jsonRes,
                                       final BusinessLogDTO dto,
                                       final Map<String, ResolutionResult> resolutions) {
        if (jsonRes != null && jsonRes.contains("\"exception\"")) {
            return resolve(dto.getException(), dto.getDefaultException(), jsonReq, jsonRes, "result", resolutions);
        }
        return resolve(dto.getValue(), dto.getDefaultValue(), jsonReq, jsonRes, "result", resolutions);
    }

    public static String resolveDescription(final String jsonReq,
                                            final String jsonRes,
                                            final BusinessLogDTO dto,
                                            final Map<String, ResolutionResult> resolutions) {
        return resolve(dto.getDescription(), dto.getDefaultDescription(), jsonReq, jsonRes, "description", resolutions);
    }

    public static DataDTO resolveData(final String jsonReq,
                                      final String jsonRes,
                                      final DataParamsDTO dtos,
                                      final Map<String, ResolutionResult> resolutions) {
        final DataDTO dataDTO = new DataDTO();
        if (dtos.getDataInOut() != null) {
            dataDTO.setDataInOut(resolveParams(jsonReq, jsonRes, dtos.getDataInOut(), "data_in_out_", resolutions));
        } else {
            dataDTO.setDataIn(resolveParams(jsonReq, jsonReq, dtos.getDataIn(), "data_in_", resolutions));
            dataDTO.setDataOut(resolveParams(jsonReq, jsonRes, dtos.getDataOut(), "data_out_", resolutions));
        }
        return dataDTO;
    }

    private static Map<String, String> resolveParams(final String jsonReq,
                                                     final String jsonRes,
                                                     final DataParamDTO[] dtos,
                                                     final String prefix,
                                                     final Map<String, ResolutionResult> resolutions) {
        if (dtos == null) return null;
        Map<String, String> map = new HashMap<>(dtos.length);
        for (DataParamDTO dto : dtos) {
            String value = resolve(dto.getValue(), dto.getDefaultValue(), jsonReq, jsonRes, prefix + dto.getKey(), resolutions);
            map.put(dto.getKey(), value);
        }
        return map;
    }

    private static String resolve(final String expression,
                                  final String expressionDefault,
                                  final String jsonReq,
                                  final String jsonRes,
                                  final String key,
                                  Map<String, ResolutionResult> resolutions) {
        if (isBlank(expression)) {
            return trim(expressionDefault);
        }
        final String normalized = normalizeInput(expression);
        return of(normalized)
                .filter(input -> input.startsWith("response") || input.startsWith("exception"))
                .map(input -> resolveDetailed(jsonRes, null, normalized, expressionDefault, expressionDefault, resolutions))
                .orElseGet(() -> resolveDetailed(jsonReq, "_fields", normalized, expressionDefault, key, resolutions));
    }

    private static String resolveDetailed(final String json,
                                          final String rootScope,
                                          final String expression,
                                          final String expressionDefault,
                                          final String key,
                                          final Map<String, ResolutionResult> resolutions) {
        ResolutionResult resolutionResult = ExpressionResolver.resolveDetailed(json, rootScope, expression, expressionDefault);
        resolutions.put(key, resolutionResult);
        return defaultIfBlank(resolutionResult.getValue(), expressionDefault);
    }
}