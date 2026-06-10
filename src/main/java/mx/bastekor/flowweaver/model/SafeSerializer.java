package mx.bastekor.flowweaver.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unchecked")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SafeSerializer {

    /**
     * Versiónn sin envolturas de {@link #safeValue}.
     * <p>
     * No añade metadatos ({@code _type}, {@code _toString}, {@code _value}).
     * Retorna el objeto crudo para que Jackson lo serialice con su tipo natural:
     * <ul>
     *   <li>String → {@code "texto"}</li>
     *   <li>Integer → {@code 42}</li>
     *   <li>Boolean → {@code true} / {@code false}</li>
     *   <li>Map / POJO → {@code { ... }} anidado sin envoltura</li>
     *   <li>Collection / Array → {@code [...]} sin envoltura</li>
     * </ul>
     * <p>
     * Cuando se alcanza {@code depth >= maxDepth} retorna un String plano
     * {@code "depth_limit_reached " + value} en lugar de un mapa con metadatos.
     *
     * @param value    objeto a serializar
     * @param depth    profundidad actual (quien llama inicia en 0)
     * @param maxDepth profundidad máxima permitida
     * @return representación cruda del objeto, null si value es null
     */
    public static Object rawValue(Object value, int depth, int maxDepth) {
        if (value == null) {
            return null;
        }

        if (depth >= maxDepth) {
            return "depth_limit_reached " + value;
        }

        if (isSimple(value)) {
            return value;
        }

        try {
            if (value instanceof Map) {
                Map<Object, Object> in = (Map<Object, Object>) value;
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<Object, Object> e : in.entrySet()) {
                    out.put(String.valueOf(e.getKey()), rawValue(e.getValue(), depth + 1, maxDepth));
                }
                return out;
            }

            if (value instanceof Collection) {
                List<Object> list = new ArrayList<>();
                for (Object o : (Collection<?>) value) {
                    list.add(rawValue(o, depth + 1, maxDepth));
                }
                return list;
            }

            if (value.getClass().isArray()) {
                int len = Array.getLength(value);
                List<Object> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(rawValue(Array.get(value, i), depth + 1, maxDepth));
                }
                return list;
            }

            if (isNotSerializable(value)) {
                return "NOT_SERIALIZABLE: " + value.getClass().getName();
            }

            Map<String, Object> fieldsMap = new LinkedHashMap<>();

            if (value instanceof Throwable t) {
                fieldsMap.put("message", t.getMessage());
                fieldsMap.put("cause", rawValue(t.getCause(), depth + 1, maxDepth));
//                fieldsMap.put("stackTrace", rawValue(t.getStackTrace(), depth + 1, maxDepth)); // Pendiente atención
            }
            for (Class<?> clazz = value.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Field f : clazz.getDeclaredFields()) {
                    Object fieldVal;
                    try {
                        f.setAccessible(true);
                        fieldVal = f.get(value);
                    } catch (Exception e) {
                        fieldVal = "[unreadable:" + f.getName() + "]";
                    }
                    fieldsMap.putIfAbsent(f.getName(), rawValue(fieldVal, depth + 1, maxDepth));
                }
            }
            return fieldsMap;

        } catch (Throwable t) {
            return "NOT_SERIALIZABLE (" + t.getClass().getSimpleName() + "): " + value;
        }
    }

    public static Object safeValue(Object value, int depth, int maxDepth) {
        if (value == null) {
            return null;
        }

        if (depth >= maxDepth) {
            Map<String, Object> meta = baseMeta(value);
            meta.put("_value", Map.of("_depth", "limit_reached"));
            return meta;
        }

        if (isSimple(value)) {
            return value.toString();
        }

        try {
            if (value instanceof Map) {
                Map<Object, Object> in = (Map<Object, Object>) value;
                Map<String, Object> out = baseMeta(value);
                Map<String, Object> inner = new LinkedHashMap<>();
                for (Map.Entry<Object, Object> e : in.entrySet()) {
                    inner.put(String.valueOf(e.getKey()), safeValue(e.getValue(), depth + 1, maxDepth));
                }
                out.put("_value", inner);
                return out;
            }

            if (value instanceof Collection) {
                Map<String, Object> out = baseMeta(value);
                List<Object> list = new ArrayList<>();
                for (Object o : (Collection<?>) value) {
                    list.add(safeValue(o, depth + 1, maxDepth));
                }
                out.put("_value", list);
                return out;
            }

            if (value.getClass().isArray()) {
                Map<String, Object> out = baseMeta(value);
                int len = Array.getLength(value);
                List<Object> list = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    list.add(safeValue(Array.get(value, i), depth + 1, maxDepth));
                }
                out.put("_value", list);
                return out;
            }

            if (isNotSerializable(value)) {
                return buildErrorMeta(value, "NOT_SERIALIZABLE");
            }

            Map<String, Object> map = baseMeta(value);
            Map<String, Object> fieldsMap = new LinkedHashMap<>();
            for (Class<?> clazz = value.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object fieldVal;
                    try {
                        fieldVal = f.get(value);
                    } catch (Exception e) {
                        fieldVal = "[unreadable:" + f.getName() + "]";
                    }
                    fieldsMap.putIfAbsent(f.getName(), safeValue(fieldVal, depth + 1, maxDepth));
                }
            }
            map.put("_value", fieldsMap);
            return map;

        } catch (Throwable t) {
            return buildErrorMeta(value, "NOT_SERIALIZABLE (" + t.getClass().getSimpleName() + ")");
        }
    }

    private static Map<String, Object> baseMeta(Object value) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("_type", value.getClass().getName());
        meta.put("_toString", value.toString());
        return meta;
    }

    private static Map<String, Object> buildErrorMeta(Object value, String errorMsg) {
        Map<String, Object> meta = baseMeta(value);
        meta.put("_value", Map.of("_depth", "error", "_toString", errorMsg));
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
        return name.startsWith("javax.servlet.")
                || name.startsWith("jakarta.servlet.");
    }
}
