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
import java.util.LinkedHashMap;
import java.util.Map;

import static mx.bastekor.flowweaver.util.Util.normalizeInput;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trim;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResolveHelper {

    // ---- Infrastructure helpers ----

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

    // ---- Expression resolution ----

    public static ResolutionResult resolve(final String expression, final String expressionDefault,
                                           final String jsonReq, final String jsonRes) {
        if (isBlank(expression)) {
            return new ResolutionResult(null, null, null, null,
                    trim(expressionDefault), 0, null, null);
        }
        final String normalized = normalizeInput(expression);
        boolean isResponse = normalized.startsWith("response") || normalized.startsWith("exception");
        String snapshot = isResponse ? jsonRes : jsonReq;
        String scope = isResponse ? null : "_fields";
        ResolutionResult result = ExpressionResolver.resolveDetailed(snapshot, scope, normalized);
        if (result.getValue() == null) {
            result.setValue(expressionDefault);
        }
        return result;
    }

    public static ResolutionResult resolveResult(final String jsonReq, final String jsonRes,
                                                 final BusinessLogDTO dto) {
        if (jsonRes != null && jsonRes.contains("\"exception\"")) {
            return resolve(dto.getException(), dto.getDefaultException(), jsonReq, jsonRes);
        }
        return resolve(dto.getValue(), dto.getDefaultValue(), jsonReq, jsonRes);
    }

    public static DataDTO buildData(final String jsonReq, final String jsonRes,
                                    final DataParamsDTO dtos) {
        Map<String, ResolutionResult> ignored = new LinkedHashMap<>();
        return buildDataWithDiagnostics(jsonReq, jsonRes, dtos, ignored);
    }

    public static DataDTO buildDataWithDiagnostics(final String jsonReq, final String jsonRes,
                                                   final DataParamsDTO dtos,
                                                   final Map<String, ResolutionResult> diagnosticsOut) {
        final DataDTO dataDTO = new DataDTO();
        if (dtos.getDataInOut() != null) {
            Map<String, String> vals = new LinkedHashMap<>();
            for (DataParamDTO p : dtos.getDataInOut()) {
                ResolutionResult r = resolve(p.getValue(), p.getDefaultValue(), jsonReq, jsonRes);
                vals.put(p.getKey(), r.getValue());
                diagnosticsOut.put("data.in_out." + p.getKey(), r);
            }
            dataDTO.setDataInOut(vals);
        } else {
            if (dtos.getDataIn() != null) {
                Map<String, String> vals = new LinkedHashMap<>();
                for (DataParamDTO p : dtos.getDataIn()) {
                    ResolutionResult r = resolve(p.getValue(), p.getDefaultValue(), jsonReq, jsonReq);
                    vals.put(p.getKey(), r.getValue());
                    diagnosticsOut.put("data.in." + p.getKey(), r);
                }
                dataDTO.setDataIn(vals);
            }
            if (dtos.getDataOut() != null) {
                Map<String, String> vals = new LinkedHashMap<>();
                for (DataParamDTO p : dtos.getDataOut()) {
                    ResolutionResult r = resolve(p.getValue(), p.getDefaultValue(), jsonReq, jsonRes);
                    vals.put(p.getKey(), r.getValue());
                    diagnosticsOut.put("data.out." + p.getKey(), r);
                }
                dataDTO.setDataOut(vals);
            }
        }
        return dataDTO;
    }
}
