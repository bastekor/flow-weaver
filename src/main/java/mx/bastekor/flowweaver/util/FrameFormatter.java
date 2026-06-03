package mx.bastekor.flowweaver.util;

import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.enums.OutputMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FrameFormatter {

    public String format(Map<String, Object> fields, FrameConfig config) {
        List<Map.Entry<String, Object>> entries = new ArrayList<>(fields.entrySet());

        entries = filterExcluded(entries, config);
        entries = filterNullsAndBlanks(entries, config);

        if (config.getOutputMode() == OutputMode.DUAL_LINE) {
            return formatDualLine(entries, config);
        }
        return formatSingleLine(entries, config);
    }

    private List<Map.Entry<String, Object>> filterExcluded(
            List<Map.Entry<String, Object>> entries, FrameConfig config) {
        if (config.getExcludedKeys() == null || config.getExcludedKeys().isEmpty()) {
            return entries;
        }
        return entries.stream()
                .filter(e -> !config.getExcludedKeys().contains(e.getKey()))
                .collect(Collectors.toList());
    }

    private List<Map.Entry<String, Object>> filterNullsAndBlanks(
            List<Map.Entry<String, Object>> entries, FrameConfig config) {
        return entries.stream()
                .filter(e -> {
                    if (config.isSkipNulls() && e.getValue() == null) return false;
                    if (config.isSkipBlanks()) {
                        String str = e.getValue() != null ? e.getValue().toString() : "";
                        return !str.trim().isEmpty();
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private String formatSingleLine(List<Map.Entry<String, Object>> entries, FrameConfig config) {
        if (config.getEntrySeparator().equals(config.getPairSeparator())) {
            throw new IllegalArgumentException(
                    "entrySeparator ('" + config.getEntrySeparator()
                            + "') and pairSeparator ('" + config.getPairSeparator()
                            + "') must differ");
        }
        return entries.stream()
                .map(e -> e.getKey() + config.getPairSeparator() + valueToString(e.getValue()))
                .collect(Collectors.joining(config.getEntrySeparator()));
    }

    private String formatDualLine(List<Map.Entry<String, Object>> entries, FrameConfig config) {
        String keys = entries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(config.getEntrySeparator()));
        String values = entries.stream()
                .map(e -> valueToString(e.getValue()))
                .collect(Collectors.joining(config.getEntrySeparator()));
        return keys + "\n" + values;
    }

    private String valueToString(Object value) {
        if (value == null) return "";
        return String.valueOf(value);
    }
}
