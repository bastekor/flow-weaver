package mx.bastekor.flowweaver.util;

import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Util {

    public static String getDuration(final Instant start, final Instant end) {
        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();
        long millis = duration.minusMinutes(minutes).minusSeconds(seconds).toMillis();
        return String.format("%dm %ds %dms", minutes, seconds, millis);
    }
}