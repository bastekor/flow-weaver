package mx.bastekor.flowweaver.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SafeSerializer {

    public static Map<String, Object> safeValue(Object value, int depth, int maxDepth) {
        if (value == null) {
            Map<String, Object> nullMap = new LinkedHashMap<>();
            nullMap.put("_type", "null");
            nullMap.put("_class", "null");
            nullMap.put("_string", "null");
            return nullMap;
        }

        if (depth >= maxDepth) {
            return buildMeta(value, "[depth_limit_reached]", true);
        }

        if (isSimple(value)) {
            return buildMeta(value, value.toString(), false);
        }

        try {
            if (value instanceof Map) {
                Map<Object, Object> in = (Map<Object, Object>) value;
                Map<String, Object> out = baseMeta(value);
                Map<String, Object> inner = new LinkedHashMap<>();
                for (Map.Entry<Object, Object> e : in.entrySet()) {
                    inner.put(String.valueOf(e.getKey()), safeValue(e.getValue(), depth + 1, maxDepth));
                }
                out.put("value", inner);
                return out;
            }

            if (value instanceof Collection) {
                Map<String, Object> out = baseMeta(value);
                List<Object> list = new ArrayList<>();
                for (Object o : (Collection<?>) value) {
                    list.add(safeValue(o, depth + 1, maxDepth));
                }
                out.put("value", list);
                return out;
            }

            if (value.getClass().isArray()) {
                Map<String, Object> out = baseMeta(value);
                int len = Array.getLength(value);
                List<Object> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(safeValue(Array.get(value, i), depth + 1, maxDepth));
                }
                out.put("value", list);
                return out;
            }

            if (isNotSerializable(value)) {
                return buildMeta(value, "NOT_SERIALIZABLE", true);
            }

            Map<String, Object> map = baseMeta(value);
            Map<String, Object> fieldsMap = new LinkedHashMap<>();
            for (Field f : value.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object fieldVal;
                try {
                    fieldVal = f.get(value);
                } catch (Exception e) {
                    fieldVal = "[unreadable:" + f.getName() + "]";
                }
                fieldsMap.put(f.getName(), safeValue(fieldVal, depth + 1, maxDepth));
            }
            map.put("value", fieldsMap);
            return map;

        } catch (Throwable t) {
            return buildMeta(value, "NOT_SERIALIZABLE (" + t.getClass().getSimpleName() + ")", true);
        }
    }

    private static Map<String, Object> baseMeta(Object value) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("_type", value.getClass().getSimpleName());
        meta.put("_class", value.getClass().getName());
        meta.put("_string", value.toString());
        return meta;
    }

    private static Map<String, Object> buildMeta(Object value, String snapshotVal, boolean markError) {
        Map<String, Object> meta = baseMeta(value);
        if (markError) {
            meta.put("_snapshot", snapshotVal);
        } else {
            meta.put("value", snapshotVal);
        }
        return meta;
    }

    private static boolean isSimple(Object o) {
        Class<?> c = o.getClass();
        return c.isPrimitive() ||
                c == String.class ||
                Number.class.isAssignableFrom(c) ||
                Boolean.class.isAssignableFrom(c) ||
                Character.class.isAssignableFrom(c) ||
                Date.class.isAssignableFrom(c) ||
                Enum.class.isAssignableFrom(c);
    }

    private static boolean isNotSerializable(Object o) {
        String name = o.getClass().getName();
        return name.startsWith("javax.servlet")
                || name.startsWith("jakarta.servlet")
                || name.contains("Request")
                || name.contains("Response");
    }
}
