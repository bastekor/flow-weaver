package mx.bastekor.flowweaver.aspect;

import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.context.FlowWeaverContext;
import mx.bastekor.flowweaver.enums.Mode;
import mx.bastekor.flowweaver.service.IBusinessLogAspectService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static mx.bastekor.flowweaver.enums.Mode.DYNAMIC;
import static mx.bastekor.flowweaver.enums.Mode.MERGED;
import static mx.bastekor.flowweaver.enums.Mode.STATIC;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class FlowWeaverAspectAuditTrailTest {

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
//        when(joinPoint.getSignature()).thenReturn(methodSignature);
        // Limpiar el contexto antes de cada prueba para evitar interferencias
        FlowWeaverContext.clearCurrentThreadContainer();
        // Mockear los métodos de audit trail para evitar excepciones
        Mockito.doNothing().when(businessLogAspectService).processAuditTrailIn(any(), any(), any());
        Mockito.doNothing().when(businessLogAspectService).processAuditTrailOut(any(), any(), any(), any(), any(), any());
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

//        when(methodSignature.getMethod()).thenReturn(method);
//        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("Audit OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("Audit OK", result);
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
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

        when(joinPoint.proceed()).thenReturn("Audit OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertEquals(String.class, result.getClass());
        assertEquals("Audit OK", result);
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withDescription() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAuditWithDescription");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals("Test Description", annotation.description());
        assertEquals("Default Description", annotation.defaultDescription());

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        assertEquals(String.class, result.getClass());
        assertEquals("OK", result);
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withValue() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAuditWithValue");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals("response", annotation.value());
        assertEquals("Default Value", annotation.defaultValue());

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withException() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAuditWithException");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals("exception.message", annotation.exception());
        assertEquals("Default Exception", annotation.defaultException());

        when(joinPoint.proceed()).thenThrow(new RuntimeException("Audit Error"));
        assertThrows(RuntimeException.class, () -> aspect.aroundAuditTrail(joinPoint, annotation));
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withModeMerged() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAuditWithMergedMode");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals(MERGED, annotation.mode());

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withDataIn() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAuditWithDataIn");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals(2, annotation.dataIn().length);
        assertEquals("key1", annotation.dataIn()[0].key());
        assertEquals("value1", annotation.dataIn()[0].value());
        assertEquals("default1", annotation.dataIn()[0].defaultValue());
        assertEquals("key2", annotation.dataIn()[1].key());
        assertEquals("value2", annotation.dataIn()[1].value());
        assertEquals("default2", annotation.dataIn()[1].defaultValue());

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withDataOut() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAuditWithDataOut");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals(2, annotation.dataOut().length);

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withDataInOut() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAuditWithDataInOut");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        assertEquals(2, annotation.dataInOut().length);

        when(joinPoint.proceed()).thenReturn("OK");
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_error() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit001");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Audit Error"));
        assertThrows(RuntimeException.class, () -> aspect.aroundAuditTrail(joinPoint, annotation));
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testConcurrency_auditTrail() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit001");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        when(joinPoint.proceed()).thenReturn("OK");

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
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        // Note: Since processAuditTrailIn and processAuditTrailOut are @Async, we verify they were called but not necessarily completed synchronously
        verify(businessLogAspectService, times(numberOfTasks)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(numberOfTasks)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testProcessBusinessLogFailure_auditTrail() throws Throwable {
        // Arrange
        Method method = TestComponent.class.getMethod("doAudit001");
        AuditTrail annotation = this.getAuditTrailAnnotation(method);
        when(joinPoint.proceed()).thenReturn("OK");

        // Act & Assert - AuditTrail doesn't call processBusinessLog, so no exception should be thrown
        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        assertEquals("OK", result);
        // AuditTrail doesn't call processBusinessLog directly - only BusinessLog does
        verify(businessLogAspectService, times(0)).processBusinessLog(any(), any(), any(), any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withNullArgs() throws Throwable {
        Method method = TestComponent.class.getMethod("doAuditWithNullArgs", String.class, String.class);
        AuditTrail annotation = this.getAuditTrailAnnotation(method);

        when(joinPoint.proceed()).thenReturn("OK");

        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aroundAuditTrail_withEmptyArgs() throws Throwable {
        Method method = TestComponent.class.getMethod("doAuditWithEmptyArgs", String.class, String.class);
        AuditTrail annotation = this.getAuditTrailAnnotation(method);

        when(joinPoint.proceed()).thenReturn("OK");

        Object result = aspect.aroundAuditTrail(joinPoint, annotation);
        assertNotNull(result);
        assertEquals("OK", result);
        verify(businessLogAspectService, times(1)).processAuditTrailIn(any(), any(), any());
        verify(businessLogAspectService, times(1)).processAuditTrailOut(any(), any(), any(), any(), any(), any());
    }

    private AuditTrail getAuditTrailAnnotation(Method method) {
        AuditTrail annotation = method.getAnnotation(AuditTrail.class);
        log.info("AuditTrail annotation: {}", annotation);
        return annotation;
    }

    // Clase simulada con métodos anotados
    static class TestComponent {

        @AuditTrail
        public String doAudit001() {
            return "Audit OK";
        }

        @AuditTrail(flowCode = "MX-001", operationCode = "AUDIT-001", mode = DYNAMIC)
        public void doAudit002(String param) {
        }

        @AuditTrail(description = "Test Description", defaultDescription = "Default Description")
        public String doAuditWithDescription() {
            return "OK";
        }

        @AuditTrail(value = "response", defaultValue = "Default Value")
        public String doAuditWithValue() {
            return "OK";
        }

        @AuditTrail(exception = "exception.message", defaultException = "Default Exception")
        public String doAuditWithException() {
            return "OK";
        }

        @AuditTrail(mode = MERGED)
        public String doAuditWithMergedMode() {
            return "OK";
        }

        @AuditTrail(dataIn = {
                @DataParam(key = "key1", value = "value1", defaultValue = "default1"),
                @DataParam(key = "key2", value = "value2", defaultValue = "default2")
        })
        public String doAuditWithDataIn() {
            return "OK";
        }

        @AuditTrail(dataOut = {
                @DataParam(key = "key1", value = "value1", defaultValue = "default1"),
                @DataParam(key = "key2", value = "value2", defaultValue = "default2")
        })
        public String doAuditWithDataOut() {
            return "OK";
        }

        @AuditTrail(dataInOut = {
                @DataParam(key = "key1", value = "value1", defaultValue = "default1"),
                @DataParam(key = "key2", value = "value2", defaultValue = "default2")
        })
        public String doAuditWithDataInOut() {
            return "OK";
        }

        // Combinaciones adicionales para cubrir todos los parámetros posibles

        @AuditTrail(flowCode = "FC-001", operationCode = "OP-001", description = "Test operation", defaultDescription = "Default operation")
        public String doAuditWithFlowCodeAndOperationCodeAndDescription() {
            return "OK";
        }

        @AuditTrail(flowCode = "FC-002", operationCode = "OP-002", value = "result", defaultValue = "Default result")
        public String doAuditWithFlowCodeAndOperationCodeAndValue() {
            return "OK";
        }

        @AuditTrail(flowCode = "FC-003", operationCode = "OP-003", exception = "ex.message", defaultException = "Default exception")
        public String doAuditWithFlowCodeAndOperationCodeAndException() {
            return "OK";
        }

        @AuditTrail(description = "Test description", value = "result", defaultValue = "Default result")
        public String doAuditWithDescriptionAndValue() {
            return "OK";
        }

        @AuditTrail(description = "Test description", exception = "ex.message", defaultException = "Default exception")
        public String doAuditWithDescriptionAndException() {
            return "OK";
        }

        @AuditTrail(value = "result", exception = "ex.message", defaultException = "Default exception")
        public String doAuditWithValueAndException() {
            return "OK";
        }

        @AuditTrail(flowCode = "FC-004", operationCode = "OP-004", description = "Test", value = "result", exception = "ex.message",
                defaultDescription = "Default desc", defaultValue = "Default val", defaultException = "Default ex")
        public String doAuditWithAllParameters() {
            return "OK";
        }

        @AuditTrail(mode = Mode.DYNAMIC, flowCode = "FC-005", operationCode = "OP-005")
        public String doAuditWithDynamicMode() {
            return "OK";
        }

        @AuditTrail(mode = Mode.MERGED, description = "Merged mode test")
        public String doAuditWithMergedModeDescription() {
            return "OK";
        }

        @AuditTrail(dataIn = {
                @DataParam(key = "singleKey", value = "singleValue", defaultValue = "singleDefault")
        })
        public String doAuditWithSingleDataIn() {
            return "OK";
        }

        @AuditTrail(dataOut = {
                @DataParam(key = "singleKey", value = "singleValue", defaultValue = "singleDefault")
        })
        public String doAuditWithSingleDataOut() {
            return "OK";
        }

        @AuditTrail(dataInOut = {
                @DataParam(key = "singleKey", value = "singleValue", defaultValue = "singleDefault")
        })
        public String doAuditWithSingleDataInOut() {
            return "OK";
        }

        @AuditTrail(dataIn = {
                @DataParam(key = "key1", value = "", defaultValue = "default1"),
                @DataParam(key = "key2", value = "value2", defaultValue = ""),
                @DataParam(key = "key3", value = "", defaultValue = "")
        })
        public String doAuditWithEmptyDataInValues() {
            return "OK";
        }

        @AuditTrail(dataOut = {
                @DataParam(key = "key1", value = "", defaultValue = "default1"),
                @DataParam(key = "key2", value = "value2", defaultValue = ""),
                @DataParam(key = "key3", value = "", defaultValue = "")
        })
        public String doAuditWithEmptyDataOutValues() {
            return "OK";
        }

        @AuditTrail(dataInOut = {
                @DataParam(key = "key1", value = "", defaultValue = "default1"),
                @DataParam(key = "key2", value = "value2", defaultValue = ""),
                @DataParam(key = "key3", value = "", defaultValue = "")
        })
        public String doAuditWithEmptyDataInOutValues() {
            return "OK";
        }

        @AuditTrail(flowCode = "FC-006", operationCode = "OP-006", description = "Test with null args")
        public String doAuditWithNullArgs(String param1, String param2) {
            return "OK";
        }

        @AuditTrail(flowCode = "FC-007", operationCode = "OP-007", description = "Test with empty args")
        public String doAuditWithEmptyArgs(String param1, String param2) {
            return "OK";
        }
    }
}