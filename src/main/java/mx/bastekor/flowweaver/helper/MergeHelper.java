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

        if (value instanceof Optional<?> opt) {
            if (opt.isEmpty()) return null;
            return (T) validate(opt.get());
        }

        if (value instanceof String s) {
            return (!s.isBlank()) ? value : null;
        }

        if (value instanceof Collection<?> c) return (!c.isEmpty()) ? value : null;
        if (value instanceof Map<?, ?> m) return (!m.isEmpty()) ? value : null;

        if (value.getClass().isArray()) {
            return (Array.getLength(value) > 0) ? value : null;
        }

        return value;
    }
}