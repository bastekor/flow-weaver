package mx.bastekor.flowweaver.util;

import java.time.Duration;
import java.time.Instant;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Util {

    public static String getDuration(final Instant start, final Instant end) {
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        long millis = duration.minusMinutes(minutes).minusSeconds(seconds).toMillis();

        String format = "";
        if (hours == 0) {
            format = String.format("%dm %ds %dms", minutes, seconds, millis);
        }

        if (minutes == 0) {
            format = String.format("%ds %dms", seconds, millis);
        }

        if (seconds == 0) {
            format = String.format("%dms", millis);
        }

        return format;
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