package mx.bastekor.flowweaver.functional.microservices.payments_service;

import mx.bastekor.flowweaver.config.FlowWeaverConfig;
import mx.bastekor.flowweaver.context.FlowWeaverContext;
import mx.bastekor.flowweaver.functional.microservices.payments_service.constant.PaymentConstants.BeneficiaryType;
import mx.bastekor.flowweaver.functional.microservices.payments_service.constant.PaymentConstants.PaymentOption;
import mx.bastekor.flowweaver.functional.microservices.payments_service.controller.PaymentController;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentRequest;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = FlowWeaverConfig.class, webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
        "flow-weaver.debug.recursive=false",
        "flow-weaver.max-depth=5"
})
class PaymentServiceTest {

    @Autowired
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        FlowWeaverContext.clearCurrentThreadContainer();
    }

    @Test
    void payOwnCard_success() {
        PaymentRequest request = new PaymentRequest(
                BeneficiaryType.OWN_CARD,
                "CLIENT-001",
                "ACC-123456",
                "CARD-789012",
                5000.00,
                PaymentOption.NO_INTEREST,
                "MXN",
                null
        );

        PaymentResponse response = controller.pay(request);

        assertNotNull(response.getTransactionId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Payment processed successfully", response.getMessage());
        assertNotNull(response.getAuthorizationCode());
        assertEquals(5000.00, response.getAmount());
        assertFalse(FlowWeaverContext.peekThreadContainerExists());
    }

    @Test
    void payThirdPartySameBank_success() {
        PaymentRequest request = new PaymentRequest(
                BeneficiaryType.THIRD_PARTY_SAME_BANK,
                "CLIENT-001",
                "ACC-123456",
                "CARD-345678",
                2500.00,
                PaymentOption.OTHER_AMOUNT,
                "MXN",
                null
        );

        PaymentResponse response = controller.pay(request);

        assertNotNull(response.getTransactionId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Payment processed successfully", response.getMessage());
        assertNotNull(response.getAuthorizationCode());
        assertEquals(2500.00, response.getAmount());
        assertFalse(FlowWeaverContext.peekThreadContainerExists());
    }

    @Test
    void payThirdPartyOtherBank_success() {
        PaymentRequest request = new PaymentRequest(
                BeneficiaryType.THIRD_PARTY_OTHER_BANK,
                "CLIENT-001",
                "ACC-123456",
                "CARD-EXT-999",
                10000.00,
                PaymentOption.MINIMUM,
                "MXN",
                null
        );

        PaymentResponse response = controller.pay(request);

        assertNotNull(response.getTransactionId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Payment processed successfully", response.getMessage());
        assertNotNull(response.getAuthorizationCode());
        assertEquals(10000.00, response.getAmount());
        assertFalse(FlowWeaverContext.peekThreadContainerExists());
    }

    @Test
    void payService_success() {
        PaymentRequest request = new PaymentRequest(
                BeneficiaryType.SERVICE,
                "CLIENT-001",
                "ACC-123456",
                null,
                800.00,
                PaymentOption.OTHER_AMOUNT,
                "MXN",
                "SERV-ELECTRIC-001"
        );

        PaymentResponse response = controller.pay(request);

        assertNotNull(response.getTransactionId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Payment processed successfully", response.getMessage());
        assertNotNull(response.getAuthorizationCode());
        assertEquals(800.00, response.getAmount());
        assertFalse(FlowWeaverContext.peekThreadContainerExists());
    }
}
