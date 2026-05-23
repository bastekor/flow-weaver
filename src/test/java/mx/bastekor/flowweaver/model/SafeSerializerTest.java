package mx.bastekor.flowweaver.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeSerializerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ===================================================================
    //  rawValue — serialización sin envolturas
    // ===================================================================

    static Stream<Arguments> rawValueSimpleSource() {
        return Stream.of(
                Arguments.of("hello", "hello"),
                Arguments.of(42, 42),
                Arguments.of(3.14, 3.14),
                Arguments.of(true, true)
        );
    }

    @ParameterizedTest
    @MethodSource("rawValueSimpleSource")
    void rawValue_simple_returnsOriginal(Object input, Object expected) {
        assertEquals(expected, SafeSerializer.rawValue(input, 0, 5));
    }

    @Test
    void rawValue_null_returnsNull() {
        assertNull(SafeSerializer.rawValue(null, 0, 5));
    }

    @Test
    void rawValue_depthLimit_concatenatesMessage() {
        String result = (String) SafeSerializer.rawValue("text", 0, 0);
        assertTrue(result.startsWith("depth_limit_reached "));
    }

    @Test
    void rawValue_POJO_noWrappers() {
        Object result = SafeSerializer.rawValue(samplePerson(), 0, 3);

        assertInstanceOf(Map.class, result, "raw POJO debe ser un Map");
        Map<?, ?> map = (Map<?, ?>) result;
        assertNull(map.get("_type"), "no debe contener _type");
        assertNull(map.get("_toString"), "no debe contener _toString");
        assertNull(map.get("_value"), "no debe contener _value");
        assertEquals("Juan", map.get("name"));
        assertEquals("juan@email.com", map.get("email"));
    }

    @Test
    void rawValue_POJO_depthLimit_returnsString() {
        Object result = SafeSerializer.rawValue(samplePerson(), 0, 0);
        assertInstanceOf(String.class, result, "en profundidad 0 debe retornar String");
        assertTrue(((String) result).startsWith("depth_limit_reached "));
    }

    @Test
    void rawValue_collection_noWrappers() {
        List<String> input = List.of("a", "b", "c");
        Object result = SafeSerializer.rawValue(input, 0, 5);
        assertInstanceOf(List.class, result, "raw Collection debe ser una List");
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    void rawValue_map_noWrappers() {
        Map<String, Integer> input = new LinkedHashMap<>();
        input.put("x", 1);
        input.put("y", 2);
        Object result = SafeSerializer.rawValue(input, 0, 5);
        assertInstanceOf(Map.class, result, "raw Map debe ser un Map");
        Map<?, ?> map = (Map<?, ?>) result;
        assertNull(map.get("_type"), "no debe contener _type");
        assertEquals(1, map.get("x"));
        assertEquals(2, map.get("y"));
    }

    @Test
    void rawValue_throwable_hasMessageAndCause() {
        Exception inner = new RuntimeException("inner");
        Exception ex = new RuntimeException("outer", inner);
        Object result = SafeSerializer.rawValue(ex, 0, 5);
        assertInstanceOf(Map.class, result);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("outer", map.get("message"));
        assertNotNull(map.get("cause"), "cause no debe ser null");
    }

    @Test
    void rawValue_customException_includesCustomField() {
        CustomEx ex = new CustomEx("msg", "ERR-001");
        Object result = SafeSerializer.rawValue(ex, 0, 5);
        assertInstanceOf(Map.class, result);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals("msg", map.get("message"));
        assertEquals("ERR-001", map.get("errorCode"));
    }

    // ===================================================================
    //  rawValue — integración con Jackson + ExpressionResolver
    // ===================================================================

    @Test
    void rawValue_customException_jsonRoundTrip() throws Exception {
        CustomEx ex = new CustomEx("fallo", "ERR-X");
        String json = MAPPER.writeValueAsString(SafeSerializer.rawValue(ex, 0, 5));
        ObjectMapper om = new ObjectMapper();
        assertEquals("fallo", om.readTree(json).get("message").asText());
        assertEquals("ERR-X", om.readTree(json).get("errorCode").asText());
    }

    // ===================================================================
    //  Helpers
    // ===================================================================

    @Getter
    @AllArgsConstructor
    static class CustomEx extends RuntimeException {
        private String errorCode;

        public CustomEx(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    @Getter
    @ToString
    @AllArgsConstructor
    static class Person {
        private String name;
        private String email;
        private Money amount;
    }

    @Getter
    @ToString
    @AllArgsConstructor
    static class Money {
        private String currency;
        private java.math.BigDecimal amount;
    }

    private static Person samplePerson() {
        return new Person("Juan", "juan@email.com",
                new Money("USD", java.math.BigDecimal.valueOf(100)));
    }
}
