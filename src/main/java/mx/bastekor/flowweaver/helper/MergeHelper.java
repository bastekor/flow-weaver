package mx.bastekor.flowweaver.helper;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface MergeHelper {

    static <T> T resolve(T priority, T fallback) {
        T validPriority = validate(priority);
        if (validPriority != null) return validPriority;

        return validate(fallback);
    }

    private static <T> T validate(T value) {
        if (value == null) return null;

        // 1. Manejo de Optionals: Si está vacío, es como si fuera nulo
        if (value instanceof Optional<?> opt) {
            if (opt.isEmpty()) return null;
            // Si no está vacío, validamos lo que tiene adentro recursivamente una vez
            return (T) validate(opt.get());
        }

        // 2. Strings (con el .isBlank() de Java 17)
        if (value instanceof String s) {
            return (!s.isBlank()) ? value : null;
        }

        // 3. Colecciones y Maps
        if (value instanceof Collection<?> c) return (!c.isEmpty()) ? value : null;
        if (value instanceof Map<?, ?> m) return (!m.isEmpty()) ? value : null;

        // 4. Arrays
        if (value.getClass().isArray()) {
            return (Array.getLength(value) > 0) ? value : null;
        }

        return value;
    }
}
