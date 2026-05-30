package mx.bastekor.flowweaver.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.model.SafeSerializer;
import mx.bastekor.flowweaver.resolver.ExpressionResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafeSnapshotMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROOT_SCOPE = "_fields";

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Test
    void mapArgs_proceedingJoinPoint_containsArgsNodes() throws Exception {
        Method method = TestService.class.getMethod("greet", String.class, String.class, int.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"hola", "mundo", 42});

        String json = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        assertTrue(json.contains("\"args0\""), "JSON debe contener args0, pero fue: " + json);
        assertTrue(json.contains("\"args1\""), "JSON debe contener args1, pero fue: " + json);
        assertTrue(json.contains("\"args2\""), "JSON debe contener args2, pero fue: " + json);
        assertTrue(json.contains("\"_args\""), "JSON debe contener _args legacy, pero fue: " + json);
        assertEquals("hola", ExpressionResolver.resolve(json, ROOT_SCOPE,"args0"));
        assertEquals("mundo", ExpressionResolver.resolve(json, ROOT_SCOPE,"args1"));
        assertEquals("42", ExpressionResolver.resolve(json, ROOT_SCOPE,"args2"));
    }

    @Test
    void mapArgs_proceedingJoinPoint_returnsErrorOnException() {
        when(joinPoint.getSignature()).thenThrow(new RuntimeException("mock error"));
        String json = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        assertTrue(json.startsWith("{"), "JSON debe ser un objeto, pero fue: " + json);
    }

    @Test
    void mapArgs_process_POJOArg_resolvesNestedField() throws Exception {
        Method method = TestService.class.getMethod("process", String.class, Person.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ABC", new Person("Juan", "juan@test.com", new Money("MXN", java.math.BigDecimal.valueOf(200)))});

        String json = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        System.out.println(json);
        assertEquals("ABC", ExpressionResolver.resolve(json, ROOT_SCOPE, "args0"));
        assertEquals("Juan", ExpressionResolver.resolve(json, ROOT_SCOPE, "args1.name"));
        assertEquals("juan@test.com", ExpressionResolver.resolve(json, ROOT_SCOPE, "args1.email"));
    }

    @Test
    void mapArgs_process2_listOfPOJOs_resolvesIndexAndField() throws Exception {
        Method method = TestService.class.getMethod("process2", String.class, List.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"XYZ", List.of(
                new Person("Alice", "alice@test.com", new Money("USD", java.math.BigDecimal.valueOf(100))),
                new Person("Bob", "bob@test.com", new Money("EUR", java.math.BigDecimal.valueOf(50)))
        )});

        String json = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        assertEquals("XYZ", ExpressionResolver.resolve(json, ROOT_SCOPE, "args0"));
        assertEquals("Alice", ExpressionResolver.resolve(json, ROOT_SCOPE, "args1[0].name"));
        assertEquals("bob@test.com", ExpressionResolver.resolve(json, ROOT_SCOPE, "args1[1].email"));
        assertEquals("EUR", ExpressionResolver.resolve(json, ROOT_SCOPE, "args1[1].amount.currency"));
    }

    // ===================================================================
    //  _fields — índice inverso de propiedades
    // ===================================================================

    @Test
    void fields_containsArgsN_andParamName() throws Exception {
        Method method = TestService.class.getMethod("greet", String.class, String.class, int.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"a", "b", 1});

        String json = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        // por argsN
        assertEquals("a", ExpressionResolver.resolve(json, "_fields.args0"));
        assertEquals("b", ExpressionResolver.resolve(json, "_fields.args1"));
        assertEquals("1", ExpressionResolver.resolve(json, "_fields.args2"));
        // por nombre de parámetro
        assertEquals("a", ExpressionResolver.resolve(json, "_fields.saludo"));
        assertEquals("b", ExpressionResolver.resolve(json, "_fields.nombre"));
        assertEquals("1", ExpressionResolver.resolve(json, "_fields.cantidad"));
    }

    @Test
    void fields_POJOArg_resolvesNestedField() throws Exception {
        Method method = TestService.class.getMethod("process", String.class, Person.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"ABC", new Person("Juan", "juan@test.com", new Money("MXN", java.math.BigDecimal.valueOf(200)))});

        String json = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        // por argsN
        assertEquals("ABC", ExpressionResolver.resolve(json, "_fields.args0"));
        assertEquals("Juan", ExpressionResolver.resolve(json, "_fields.args1.name"));
        assertEquals("juan@test.com", ExpressionResolver.resolve(json, "_fields.args1.email"));
        assertEquals("MXN", ExpressionResolver.resolve(json, "_fields.args1.amount.currency"));
        // por nombre de parámetro
        assertEquals("ABC", ExpressionResolver.resolve(json, "_fields.code"));
        assertEquals("Juan", ExpressionResolver.resolve(json, "_fields.person.name"));
        assertEquals("juan@test.com", ExpressionResolver.resolve(json, "_fields.person.email"));
    }

    @Test
    void fields_withThreeParams_containsAll() throws Exception {
        Method method = TestService.class.getMethod("placeOrder", String.class, Person.class, String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{
                "ORD-001",
                new Person("Alice", "alice@test.com", new Money("USD", java.math.BigDecimal.valueOf(100))),
                "handle with care"
        });

        String json = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        // por argsN
        assertEquals("ORD-001", ExpressionResolver.resolve(json, "_fields.args0"));
        assertEquals("Alice", ExpressionResolver.resolve(json, "_fields.args1.name"));
        assertEquals("handle with care", ExpressionResolver.resolve(json, "_fields.args2"));
        // por nombre de parámetro
        assertEquals("ORD-001", ExpressionResolver.resolve(json, "_fields.orderId"));
        assertEquals("Alice", ExpressionResolver.resolve(json, "_fields.customer.name"));
        assertEquals("handle with care", ExpressionResolver.resolve(json, "_fields.notes"));
        assertTrue(json.contains("\"_fields\""), "JSON debe contener _fields");
    }

    // ===================================================================
    //  mapArgs — formato argsN construido con rawValue (helper)
    // ===================================================================

    @Test
    void mapArgs_rawNodeString_resolves() throws Exception {
        String json = buildSnapshot(5, "Hello");
        assertEquals("Hello", ExpressionResolver.resolve(json, "args0"));
    }

    @Test
    void mapArgs_rawNodeMap_resolvesField() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "Juan");
        data.put("age", 30);
        String json = buildSnapshot(5, data);
        assertEquals("Juan", ExpressionResolver.resolve(json, "args0.name"));
        assertEquals("30", ExpressionResolver.resolve(json, "args0.age"));
    }

    @Test
    void mapArgs_rawNodeList_resolvesIndex() throws Exception {
        String json = buildSnapshot(5, List.of("a", "b", "c"));
        assertEquals("a", ExpressionResolver.resolve(json, "args0[0]"));
        assertEquals("c", ExpressionResolver.resolve(json, "args0[2]"));
    }

    // ===================================================================
    //  mapObject — serialización de response / exception
    // ===================================================================

    @Test
    void mapObject_response_resolvesField() {
        Person p = new Person("Alice", "alice@mail.com", new Money("MXN", java.math.BigDecimal.valueOf(500)));
        String json = SafeSnapshotMapper.mapObject(p, 5);
        System.out.println(json);
        assertTrue(json.contains("\"response\""), "JSON debe contener nodo response");
        assertEquals("Alice", ExpressionResolver.resolve(json, "response.name"));
        assertEquals("alice@mail.com", ExpressionResolver.resolve(json, "response.email"));
    }

    @Test
    void mapObject_exception_resolvesMessage() {
        String json = SafeSnapshotMapper.mapObject(new RuntimeException("Algo salió mal"), 5);
        assertTrue(json.contains("\"exception\""), "JSON debe contener nodo exception");
        assertEquals("Algo salió mal", ExpressionResolver.resolve(json, "exception.message"));
    }

    @Test
    void mapObject_customException_resolvesCustomField() {
        String json = SafeSnapshotMapper.mapObject(new CustomEx("fail", "ERR-999"), 5);
        assertEquals("ERR-999", ExpressionResolver.resolve(json, "exception.errorCode"));
    }

    @Test
    void mapObject_null_returnsEmptyObject() {
        String json = SafeSnapshotMapper.mapObject(null, 5);
        assertEquals("{}", json);
    }

    @Test
    void mapObject_depthLimit_returnsString() {
        Person p = new Person("Deep", "deep@test.com", new Money("USD", java.math.BigDecimal.ONE));
        String json = SafeSnapshotMapper.mapObject(p, 0);
        String value = ExpressionResolver.resolve(json, "response");
        assertTrue(value != null && value.startsWith("depth_limit_reached "),
                "depth 0 debe retornar mensaje de límite, pero fue: " + value);
    }

    @Test
    void mapObject_error_returnsErrorJson() {
        String json = SafeSnapshotMapper.mapObject(null, 5);
        assertNotNull(json);
    }

    // ===================================================================
    //  Helper — construye JSON en el mismo formato que mapArgs
    // ===================================================================

    private static String buildSnapshot(int maxDepth, Object arg0) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("args0", SafeSerializer.rawValue(arg0, 0, maxDepth));
        return MAPPER.writeValueAsString(root);
    }

    private static String buildSnapshot(int maxDepth, Object arg0, Object arg1) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("args0", SafeSerializer.rawValue(arg0, 0, maxDepth));
        root.put("args1", SafeSerializer.rawValue(arg1, 0, maxDepth));
        return MAPPER.writeValueAsString(root);
    }

    // ===================================================================
    //  Clases de prueba
    // ===================================================================

    static class TestService {
        @BusinessLog
        public String greet(String saludo, String nombre, int cantidad) {
            return "OK";
        }

        @BusinessLog
        public String process(String code, Person person) {
            return "OK";
        }

        @BusinessLog
        public String process2(String code, List<Person> persons) {
            return "OK";
        }

        @BusinessLog
        public String placeOrder(String orderId, Person customer, String notes) {
            return "OK";
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

    @Getter
    @AllArgsConstructor
    static class CustomEx extends RuntimeException {
        private String errorCode;

        public CustomEx(String message, String errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}
