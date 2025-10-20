package mx.bastekor.flowweaver.util;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class UtilTest {

    @Test
    void testGetDuration_ZeroDuration() {
        Instant now = Instant.now();
        String result = Util.getDuration(now, now);
        assertEquals("0ms", result);
    }

    @Test
    void testGetDuration_OnlyMilliseconds() {
        Instant start = Instant.parse("2024-01-01T00:00:00.000Z");
        Instant end = Instant.parse("2024-01-01T00:00:00.123Z");
        String result = Util.getDuration(start, end);
        assertEquals("123ms", result);
    }

    @Test
    void testGetDuration_SecondsAndMilliseconds() {
        Instant start = Instant.parse("2024-01-01T00:00:00.000Z");
        Instant end = Instant.parse("2024-01-01T00:00:05.250Z");
        String result = Util.getDuration(start, end);
        assertEquals("5s 250ms", result);
    }

    @Test
    void testGetDuration_MinutesSecondsMilliseconds() {
        Instant start = Instant.parse("2024-01-01T00:00:00.000Z");
        Instant end = Instant.parse("2024-01-01T00:02:03.456Z");
        String result = Util.getDuration(start, end);
        assertEquals("2m 3s 456ms", result);
    }

    @Test
    void testGetDuration_EndBeforeStart() {
        Instant start = Instant.parse("2024-01-01T00:00:10.000Z");
        Instant end = Instant.parse("2024-01-01T00:00:00.000Z");
        String result = Util.getDuration(start, end);
        assertEquals("-10s 0ms", result);
    }
}