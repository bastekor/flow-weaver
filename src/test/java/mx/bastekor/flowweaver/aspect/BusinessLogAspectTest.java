package mx.bastekor.flowweaver.aspect;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.service.BusinessLogAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.List;

import static mx.bastekor.flowweaver.enums.Mode.DYNAMIC;
import static mx.bastekor.flowweaver.enums.Mode.MERGED;
import static mx.bastekor.flowweaver.enums.Mode.STATIC;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class BusinessLogAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private BusinessLogAspectService businessLogAspectService;

    @InjectMocks
    private BusinessLogAspect aspect;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);

    }

    @Test
    void doSomething001() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomething001");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals(EMPTY, annotation.operationCode());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(STATIC, annotation.mode());
        assertEquals(0, annotation.dataOut().length);

        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.around(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).enqueue(any());
    }

    @Test
    void doSomething002() throws Throwable {
        Method method = TestComponent.class.getMethod("doSomething002", int.class, String.class, List.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals(EMPTY, annotation.operationCode());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(MERGED, annotation.mode());
        assertEquals(0, annotation.dataOut().length);

        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Hola", List.of("1", "2")});
        when(joinPoint.proceed()).thenReturn(null);
        Object result = aspect.around(joinPoint, annotation);
        assertNull(result);
        verify(businessLogAspectService, times(1)).enqueue(any());
    }

    @Test
    void doSomething003() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomething003");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals("MX-001", annotation.operationCode());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(DYNAMIC, annotation.mode());
        assertEquals(0, annotation.dataOut().length);

        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.around(joinPoint, annotation);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
    }

    @Test
    void doSomething004() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomething004", int.class, String.class, List.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        assertEquals("ES-002", annotation.operationCode());
        assertEquals(EMPTY, annotation.description());
        assertEquals("Español 2, latino", annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(STATIC, annotation.mode());
        assertEquals(0, annotation.dataOut().length);

        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Hola", List.of("3", "4")});
        when(joinPoint.proceed()).thenReturn(null);
        Object result = aspect.around(joinPoint, annotation);
        assertNull(result);
    }

    @Test
    void doSomething004_error() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomething004", int.class, String.class, List.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Hola", List.of("3", "4")});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test Bitacora Error"));
        assertThrows(RuntimeException.class, () -> aspect.around(joinPoint, annotation));
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

        @BusinessLog(mode = MERGED)
        public void doSomething002(int enteroInt, String cadenaString, @Nullable @NonNull List<?> listaDesconocida) {
        }

        @BusinessLog(operationCode = "MX-001", mode = DYNAMIC)
        public String doSomething003() {
            return "OK";
        }

        @BusinessLog(operationCode = "ES-002", defaultDescription = "Español 2, latino")
        public void doSomething004(int i, String str, List<?> list) {
        }
    }
}