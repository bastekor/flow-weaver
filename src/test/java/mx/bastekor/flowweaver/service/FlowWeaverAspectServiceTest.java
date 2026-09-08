package mx.bastekor.flowweaver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.annotation.DataParam;
import mx.bastekor.flowweaver.config.FlowWeaverRootConfig;
import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.enums.Phase;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.handler.FlowWeaverResultHandler;
import mx.bastekor.flowweaver.mapper.RequestDTOMapper;
import mx.bastekor.flowweaver.mapper.SafeSnapshotMapper;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.model.SafeSerializer;

import mx.bastekor.flowweaver.util.FrameExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowWeaverAspectServiceTest {

    private static final String MC = json("/snapshots/method-contract.jsonc");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private Environment environment;
    @Mock
    private FrameExtractor frameExtractor;
    @Mock
    private RequestDTOMapper requestDTOMapper;
    @Mock
    private FlowWeaverRootConfig flowWeaverRootConfig;
    @Mock
    private FrameConfig frameConfig;
    @Mock
    private FlowWeaverResultHandler flowWeaverResultHandler;

    @InjectMocks
    private FlowWeaverAspectService flowWeaverAspectService;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @BeforeEach
    void setup() {
        when(flowWeaverRootConfig.getMaxDepth()).thenReturn(5);
        when(frameExtractor.extract(any(), anyInt())).thenReturn(new LinkedHashMap<>());
    }

    private static String json(String resource) {
        try {
            var is = Objects.requireNonNull(
                    FlowWeaverAspectServiceTest.class.getResourceAsStream(resource),
                    "Resource not found: " + resource);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + resource, e);
        }
    }

    @Test
    void test() throws Exception {

        Method method = PaymentComponent.class.getMethod("paymentByDebitCard", String.class, double.class, String.class, String.class);
        BusinessLog businessLog = this.getBusinessLogAnnotation(method);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Paguitos NU", 150.0, "5234123443219876", "55443456789988987654"});

        String uuid = UUID.randomUUID().toString();
        when(requestDTOMapper.resolveBusinessLog(any(BusinessLogContainer.class)))
                .thenAnswer(invocation -> new BusinessLogDTO());
        when(requestDTOMapper.build(any(BusinessLogContainer.class), any(BusinessLogDTO.class)))
                .thenAnswer(invocation -> {
                    RequestDTO dto = new RequestDTO();
                    dto.setId(uuid);
                    dto.setGroup("GROUP");
                    dto.setCode("MX-002");
                    dto.setDescription("Pago con tarjeta de debito");
                    dto.setStatus(StatusEnum.SOURCE_SUCCESS.name());
                    dto.setResult("en MXN");
                    dto.setMode("STATIC");
                    dto.setPhase(Phase.EXIT);
                    return dto;
                });

        Person person = new Person("Juan", "juan@test.com", new Money("MXN", java.math.BigDecimal.valueOf(200)));
        String request = SafeSnapshotMapper.mapArgs(joinPoint, 5);
        String response = SafeSnapshotMapper.mapObject(person, 3);
        BusinessLogContainer businessLogContainer = new BusinessLogContainer(uuid, "GROUP", "CODE");
        businessLogContainer.setBusinessLog(businessLog);
        businessLogContainer.setStatus(StatusEnum.SOURCE_SUCCESS);
        businessLogContainer.setExitSignature(request);
        businessLogContainer.setResponse(response);

        flowWeaverAspectService.processBusinessLog(businessLogContainer);
    }

    private static String buildSnapshot(int maxDepth, Object arg0) throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("args0", SafeSerializer.rawValue(arg0, 0, maxDepth));
        return MAPPER.writeValueAsString(root);
    }

    private BusinessLog getBusinessLogAnnotation(Method method) {
        BusinessLog annotation = method.getAnnotation(BusinessLog.class);
        return annotation;
    }

    static class PaymentComponent {

        @BusinessLog(
                code = "MX-002",
                description = "arg[0]",
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