package mx.bastekor.flowweaver.aspect;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.FLOW_WEAVER_CONTEXT_ID;
import static mx.bastekor.flowweaver.enums.Mode.DYNAMIC;
import static mx.bastekor.flowweaver.enums.Mode.MERGED;
import static mx.bastekor.flowweaver.enums.Mode.STATIC;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FlowWeaverAspectTest {

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
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    void aroundBusinessLog_success() throws Throwable {
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
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void aroundBusinessLog_withOperationCode() throws Throwable {
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
        Object result = aspect.aroundBusinessLog(joinPoint, annotation);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void aroundBusinessLog_error() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomething004", int.class, String.class, List.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Hola", List.of("3", "4")});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test Bitacora Error"));
        assertThrows(RuntimeException.class, () -> aspect.aroundBusinessLog(joinPoint, annotation));
        verify(businessLogAspectService, times(1)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void aroundAuditTrail_success() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit001");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals(EMPTY, annotation.flowCode());
        assertEquals(EMPTY, annotation.operationCode());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(STATIC, annotation.mode());
        assertEquals(0, annotation.dataIn().length);
        assertEquals(0, annotation.dataOut().length);
        assertEquals(0, annotation.dataInOut().length);

        when(joinPoint.proceed()).thenReturn("Audit OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("Audit OK", result);
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void aroundAuditTrail_withFlowCode() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit002", String.class);
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals("MX-001", annotation.flowCode());
        assertEquals("AUDIT-001", annotation.operationCode());
        assertEquals(EMPTY, annotation.description());
        assertEquals(EMPTY, annotation.defaultDescription());
        assertEquals(EMPTY, annotation.value());
        assertEquals(EMPTY, annotation.defaultValue());
        assertEquals(EMPTY, annotation.exception());
        assertEquals(EMPTY, annotation.defaultException());
        assertEquals(DYNAMIC, annotation.mode());
        assertEquals(0, annotation.dataIn().length);
        assertEquals(0, annotation.dataOut().length);
        assertEquals(0, annotation.dataInOut().length);

        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestComponent());
        lenient().when(joinPoint.proceed()).thenReturn(null);
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNull(result);
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void aroundAuditTrail_error() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit001");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestComponent());
        lenient().when(joinPoint.proceed()).thenThrow(new RuntimeException("Audit Error"));
        assertThrows(RuntimeException.class, () -> aspect.aroundAuditTrail(joinPoint, annotation));
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void testConcurrency_businessLog() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomething004", int.class, String.class, List.class);
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{1, "Hola", List.of("3", "4")});
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn(null);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfTasks = 20;

        // Act
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
        verify(businessLogAspectService, times(numberOfTasks)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void testConcurrency_auditTrail() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit001");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestComponent());
        lenient().when(joinPoint.proceed()).thenReturn("OK");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfTasks = 20;

        // Act
        for (int i = 0; i < numberOfTasks; i++) {
            executor.submit(() -> {
                try {
                    Object result = aspect.aroundAuditTrail(joinPoint, annotation);
                    assertEquals("OK", result);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        // Assert
        assertTrue(finished, "All tasks should complete without timeout");
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void testProcessBusinessLogFailure_businessLog() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doSomething001");
        BusinessLog annotation = this.getBusinessLogAnnotation(method);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("OK");

        // Mock processBusinessLog to always throw
        Mockito.doThrow(new RuntimeException("Process failed"))
            .when(businessLogAspectService).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> aspect.aroundBusinessLog(joinPoint, annotation));
        verify(businessLogAspectService, times(1)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    @Test
    void testProcessBusinessLogFailure_auditTrail() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit001");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        lenient().when(methodSignature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestComponent());
        lenient().when(joinPoint.proceed()).thenReturn("OK");

        // Act & Assert - AuditTrail doesn't call processBusinessLog, so no exception should be thrown
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        assertEquals("OK", result);
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(BusinessLogEvent.class), any(StatusEnum.class), anyString());
    }

    private BusinessLog getBusinessLogAnnotation(Method method) {
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        log.info("BusinessLog annotation: {}", annotation);
        return annotation;
    }

    private AuditTrail getAuditTrailAnnotation(Method method) {
        AuditTrail annotation = method.getAnnotation(AuditTrail.class);
        log.info("AuditTrail annotation: {}", annotation);
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

        @AuditTrail
        public String doAudit001() {
            return "Audit OK";
        }

        @AuditTrail(flowCode = "MX-001", operationCode = "AUDIT-001", mode = DYNAMIC)
        public void doAudit002(String param) {
        }
    }
}