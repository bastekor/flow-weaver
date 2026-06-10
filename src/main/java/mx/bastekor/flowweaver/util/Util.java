package mx.bastekor.flowweaver.util;

import java.time.Duration;
import java.time.Instant;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Util {

    public static String getDuration(final Instant start, final Instant end) {
        Duration duration = Duration.between(start, end);
        long totalMillis = Math.abs(duration.toMillis());
        String sign = duration.isNegative() ? "-" : "";

        long hours = totalMillis / 3_600_000;
        long minutes = (totalMillis % 3_600_000) / 60_000;
        long seconds = (totalMillis % 60_000) / 1_000;
        long millis = totalMillis % 1_000;

        if (hours > 0) {
            return sign + String.format("%dh %dm %ds %dms", hours, minutes, seconds, millis);
        }
        if (minutes > 0) {
            return sign + String.format("%dm %ds %dms", minutes, seconds, millis);
        }
        if (seconds > 0) {
            return sign + String.format("%ds %dms", seconds, millis);
        }
        return sign + millis + "ms";
    }

    /**
     * Esté método normaliza la entrada de datos transformandolo a modo de nuestro uso. ejemplos en los pasos
     * </br>
     * 1. arg[0] → args0, args[1] → args1, arg_2 → args2, args-3 → args3</br>
     * 2. arg0 → args0, ARGS1 → args1</br>
     * 3. Arg0 → args0</br>
     * @param expression Expresión de entrada a resolver
     * @return Valor normalizado
     */
    public static String normalizeInput(final String expression) {
        int dotIdx = expression.indexOf('.');
        String first = (dotIdx >= 0) ? expression.substring(0, dotIdx) : expression;
        String rest = (dotIdx >= 0) ? expression.substring(dotIdx) : "";

        return first.replaceAll("[\\[\\]_\\-]", "")
                .replaceFirst("(?i)^args?", "args")
                + rest;
    }
}