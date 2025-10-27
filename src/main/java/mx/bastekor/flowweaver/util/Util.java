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
}