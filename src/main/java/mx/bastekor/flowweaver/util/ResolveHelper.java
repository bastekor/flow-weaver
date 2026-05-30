package mx.bastekor.flowweaver.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.DataDTO;
import mx.bastekor.flowweaver.dto.DataParamDTO;
import mx.bastekor.flowweaver.dto.DataParamsDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import org.springframework.core.env.Environment;

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

    // ============================================================
    //  Infrastructure
    // ============================================================

    public static void getHostNameAndIpAddress(final RequestDTO requestDTO) {
        try {
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            requestDTO.setHostName(hostName);
            String ipAddress = java.net.InetAddress.getLocalHost().getHostAddress();
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

    public static void fillAppInfo(final RequestDTO requestDTO, final Environment environment) {
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

    public static void fillInfrastructureInfo(final RequestDTO requestDTO, final Environment environment) {
        requestDTO.setRegion(getPropertyValue(
                new String[]{"cloud.region", "CLOUD_REGION"}, null, true, environment));
        requestDTO.setZone(getPropertyValue(
                new String[]{"cloud.zone", "CLOUD_ZONE"}, null, true, environment));
        requestDTO.setInstanceId(getPropertyValue(
                new String[]{"cloud.instance.id", "CLOUD_INSTANCE_ID"}, null, true, environment));
    }

    // ============================================================
    //  Expression resolution
    // ============================================================

    public static String resolve(final String expression, final String expressionDefault,
                                 final String jsonReq, final String jsonRes) {
        if (isBlank(expression)) {
            return trim(expressionDefault);
        }
        final String normalized = normalizeInput(expression);
        return defaultIfBlank(
                of(normalized)
                        .filter(input -> input.startsWith("response") || input.startsWith("exception"))
                        .map(input -> mx.bastekor.flowweaver.resolver.ExpressionResolver.resolve(jsonRes, normalized))
                        .orElseGet(() -> mx.bastekor.flowweaver.resolver.ExpressionResolver.resolve(jsonReq, "_fields", normalized)),
                expressionDefault
        );
    }

    public static String resolveResult(final String jsonReq, final String jsonRes,
                                       final BusinessLogDTO dto) {
        if (jsonRes != null && jsonRes.contains("\"exception\"")) {
            return resolve(dto.getException(), dto.getDefaultException(), jsonReq, jsonRes);
        }
        return resolve(dto.getValue(), dto.getDefaultValue(), jsonReq, jsonRes);
    }

    public static DataDTO buildData(final String jsonReq, final String jsonRes,
                                    final DataParamsDTO dtos) {
        final DataDTO dataDTO = new DataDTO();
        if (dtos.getDataInOut() != null) {
            dataDTO.setDataInOut(resolveParams(jsonReq, jsonRes, dtos.getDataInOut()));
        } else {
            dataDTO.setDataIn(resolveParams(jsonReq, jsonReq, dtos.getDataIn()));
            dataDTO.setDataOut(resolveParams(jsonReq, jsonRes, dtos.getDataOut()));
        }
        return dataDTO;
    }

    private static Map<String, String> resolveParams(final String jsonReq, final String jsonRes,
                                                      final DataParamDTO[] dtos) {
        if (dtos == null) return null;
        Map<String, String> map = new HashMap<>(dtos.length);
        for (DataParamDTO dto : dtos) {
            map.put(dto.getKey(), resolve(dto.getValue(), dto.getDefaultValue(), jsonReq, jsonRes));
        }
        return map;
    }
}
