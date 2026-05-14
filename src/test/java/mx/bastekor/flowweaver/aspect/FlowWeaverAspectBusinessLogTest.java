package mx.bastekor.flowweaver.aspect;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.context.FlowWeaverContext;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static mx.bastekor.flowweaver.enums.Mode.DYNAMIC;
import static mx.bastekor.flowweaver.enums.Mode.MERGED;
import static mx.bastekor.flowweaver.enums.Mode.STATIC;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FlowWeaverAspectBusinessLogTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private IBusinessLogAspectService businessLogAspectService;

    @InjectMocks
    private FlowWeaverAspect aspect;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(aspect, "bool", true);
        ReflectionTestUtils.setField(aspect, "maxDepth", 5);
        // Limpiar el contexto antes de cada prueba para evitar interferencias
        FlowWeaverContext.clearCurrentThreadContainer();
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    void aroundBusinessLog_doSomething001() throws Throwable {

        // método doSomething001 anotado con @BusinessLog sin parámetros
        Method method = TestComponent.class.getMethod("doSomething001");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals(EMPTY, annotation.code());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(STATIC, annotation.mode());
        assertEquals(0, annotation.dataOut().length);

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_doSomething002() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething002", int.class, String.class, List.class, Map.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals(EMPTY, annotation.code());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(MERGED, annotation.mode());
        assertEquals(0, annotation.dataOut().length);

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Test", new ArrayList<>(), new HashMap<>()});

        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withCode() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething003");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals("MX-001", annotation.code());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(DYNAMIC, annotation.mode());
        assertEquals(0, annotation.dataOut().length);

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withDescription() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomethingWithDescription");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals("Test Description", annotation.description());
        assertEquals("Default Description", annotation.defaultDescription());

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withValue() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomethingWithValue");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals("response", annotation.value());
        assertEquals("Default Value", annotation.defaultValue());

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withException() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomethingWithException");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals("exception.message", annotation.exception());
        assertEquals("Default Exception", annotation.defaultException());

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withModeMerged() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomethingWithMergedMode");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals(MERGED, annotation.mode());

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withDataOut() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomethingWithDataOut");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals(2, annotation.dataOut().length);
        assertEquals("key1", annotation.dataOut()[0].key());
        assertEquals("value1", annotation.dataOut()[0].value());
        assertEquals("default1", annotation.dataOut()[0].defaultValue());
        assertEquals("key2", annotation.dataOut()[1].key());
        assertEquals("value2", annotation.dataOut()[1].value());
        assertEquals("default2", annotation.dataOut()[1].defaultValue());

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_error() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething004", int.class, String.class, List.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);

        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test Bitacora Error"));
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Test", new ArrayList<>(), new HashMap<>()});
        assertThrows(RuntimeException.class, () -> aspect.aroundBusinessLog(joinPoint, annotation));
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void testConcurrency_businessLog() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething004", int.class, String.class, List.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        when(joinPoint.proceed()).thenReturn(null);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Test", new ArrayList<>(), new HashMap<>()});

        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfTasks = 20;

        for (int i = 0; i < numberOfTasks; i++) {
            executor.submit(() -> {
                try {
                    Object result = aspect.aroundBusinessLog(joinPoint, annotation);
                    assertNull(result);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert
        assertTrue(finished, "All tasks should complete without timeout");
        // Note: Since processBusinessLog is @Async, we verify it was called but not necessarily completed synchronously
        verify(businessLogAspectService, times(numberOfTasks)).processBusinessLog(any());
    }

    @Disabled("Deshabilitado debido a que aún no se constuye lógica de error de libreria")
    @Test
    void testProcessBusinessLogFailure_businessLog() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething001");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);

        // Mock processBusinessLog to always throw
        Mockito.doThrow(new RuntimeException("Process failed")).when(businessLogAspectService).processBusinessLog(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.aroundBusinessLog(joinPoint, annotation));
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_paymentByDebitCard() throws Throwable {
        Method method = PaymentComponent.class.getMethod("paymentByDebitCard", String.class, double.class, String.class, String.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals("description", annotation.description());
        assertEquals("Pago con tarjeta de debito", annotation.defaultDescription());
        assertEquals("amount", annotation.value());
        assertEquals("en MXN", annotation.defaultValue());
        assertEquals("exception.message", annotation.exception());
        assertEquals("Error en el pago con tdd", annotation.defaultException());
        assertEquals(2, annotation.dataOut().length);
        assertEquals("accountNumber", annotation.dataOut()[0].key());
        assertEquals("accountNumber", annotation.dataOut()[0].value());
        assertEquals("0", annotation.dataOut()[0].defaultValue());
        assertEquals("cardNumber", annotation.dataOut()[1].key());
        assertEquals("cardNumber", annotation.dataOut()[1].value());
        assertEquals("0", annotation.dataOut()[1].defaultValue());

        when(joinPoint.proceed()).thenReturn(true);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"1234567890", 100.0, "MXN", "MX"});
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        assertTrue((boolean) result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withArgs() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething002", int.class, String.class, List.class, Map.class);
        when(methodSignature.getMethod()).thenReturn(method);
        List<Person> persons = new ArrayList<>();

        Money mPerson1 = new Money("USD", new BigDecimal(100));
        Person person1 = new Person("Test A", "test.a@email.com", mPerson1);

        Money mPerson2 = new Money("MXN", new BigDecimal(10000));
        Person person2 = new Person("Test B", "test.b@email.com", mPerson2);

        persons.add(person1);
        persons.add(person2);

        Map<String, Object> objectHashMap = new HashMap<>();
        objectHashMap.put("texto", "String");
        objectHashMap.put("entero", 1);
        objectHashMap.put("objeto", mPerson1);
        objectHashMap.put("persons", persons);

        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Test", persons, objectHashMap});
        BusinessLog annotation = this.getBusinessLogAnnotation(method);

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    @Test
    void aroundBusinessLog_withEmptyArgs() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething002", int.class, String.class, List.class, Map.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);

        when(joinPoint.proceed()).thenReturn("OK");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{0, EMPTY, new ArrayList<>(), new HashMap<>()});
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any());
    }

    private BusinessLog getBusinessLogAnnotation(Method method) {
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        log.info("BusinessLog annotation: {}", annotation);
        return annotation;
    }

    // Clase simulada con métodos anotados
    static class TestComponent {

        @BusinessLog
        public String doSomething001() {
            return "OK";
        }

        @Async
        @BusinessLog(mode = MERGED)
        public void doSomething002(int enteroInt, String cadenaString, @NonNull @Nullable List<?> listaDesconocida, @NonNull Map<String, Object> mapaDeCosas) {
        }

        @BusinessLog(code = "MX-001", mode = DYNAMIC)
        public String doSomething003() {
            return "OK";
        }

        @BusinessLog(code = "ES-002", defaultDescription = "Español 2, latino")
        public void doSomething004(int i, String str, List<?> list) {
        }

        @BusinessLog(description = "Test Description", defaultDescription = "Default Description")
        public String doSomethingWithDescription() {
            return "OK";
        }

        @BusinessLog(value = "response", defaultValue = "Default Value")
        public String doSomethingWithValue() {
            return "OK";
        }

        @BusinessLog(exception = "exception.message", defaultException = "Default Exception")
        public String doSomethingWithException() {
            return "OK";
        }

        @BusinessLog(dataOut = {
                @DataParam(key = "key1", value = "value1", defaultValue = "default1"),
                @DataParam(key = "key2", value = "value2", defaultValue = "default2")
        })
        public String doSomethingWithDataOut() {
            return "OK";
        }

        @BusinessLog(mode = Mode.MERGED, description = "Merged mode test")
        public String doSomethingWithMergedMode() {
            return "OK";
        }
    }

    static class PaymentComponent {

        @BusinessLog(
                code = "MX-002",
                description = "description",
                defaultDescription = "Pago con tarjeta de debito",
                value = "amount",
                defaultValue = "en MXN",
                exception = "exception.message",
                defaultException = "Error en el pago con tdd",
                dataOut = {
                        @DataParam(key = "accountNumber", value = "accountNumber", defaultValue = "0"),
                        @DataParam(key = "cardNumber", value = "cardNumber", defaultValue = "0")
                }
        )
        public boolean paymentByDebitCard(String description, double amount, String accountNumber, String cardNumber) {
            return true;
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
        private BigDecimal amount;
    }
}