package mx.bastekor.flowweaver.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@Component
public class FrameExtractor {

    public Map<String, Object> extract(Object obj, int maxDepth) {
        Map<String, Object> result = new LinkedHashMap<>();
        this.extractRecursive(EMPTY, obj, result, 0, maxDepth);
        return result;
    }

    private void extractRecursive(String prefix, Object obj, Map<String, Object> result, int depth, int maxDepth) {
        if (obj == null || depth > maxDepth) return;

        Class<?> clazz = obj.getClass();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (this.isSkippable(field)) continue;

                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    String key = this.resolveKey(field);
                    String fullKey = prefix.isEmpty() ? key : prefix + "_" + key;

                    if (value == null) {
                        result.put(fullKey, null);
                    } else if (isMapType(value.getClass())) {
                        this.expandMap(fullKey, (Map<?, ?>) value, result);
                    } else if (isContainerType(value.getClass())) {
                        this.extractRecursive(fullKey, value, result, depth + 1, maxDepth);
                    } else {
                        result.put(fullKey, value);
                    }
                } catch (Exception e) {
                    log.warn("Cannot read field '{}': {}", field.getName(), e.getMessage());
                }
            }
        }
    }

    private boolean isSkippable(Field field) {
        int mod = field.getModifiers();
        if (Modifier.isStatic(mod)) return true;
        if (field.isSynthetic()) return true;
        if (Modifier.isTransient(mod)) return true;
        JsonIgnore ignore = field.getAnnotation(JsonIgnore.class);
        return ignore != null;
    }

    private String resolveKey(Field field) {
        JsonProperty jp = field.getAnnotation(JsonProperty.class);
        if (jp != null && !jp.value().isEmpty()) {
            return jp.value();
        }
        return field.getName();
    }

    private boolean isMapType(Class<?> type) {
        return Map.class.isAssignableFrom(type);
    }

    private boolean isContainerType(Class<?> type) {
        return !this.isLeafType(type);
    }

    private boolean isLeafType(Class<?> type) {
        if (type.isPrimitive()) return true;
        if (type == String.class) return true;
        if (Number.class.isAssignableFrom(type)) return true;
        if (type == Boolean.class) return true;
        if (type == Character.class) return true;
        if (type.isEnum()) return true;
        if (Date.class.isAssignableFrom(type)) return true;
        if (type.getName().startsWith("java.time.")) return true;
        if (type.isArray()) return true;
        if (Collection.class.isAssignableFrom(type)) return true;
        return false;
    }

    private void expandMap(String prefix, Map<?, ?> map, Map<String, Object> result) {
        if (map != null) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() != null ? entry.getKey().toString() : "null";
                result.put(prefix + "_" + key, entry.getValue());
            }
        }
    }
}