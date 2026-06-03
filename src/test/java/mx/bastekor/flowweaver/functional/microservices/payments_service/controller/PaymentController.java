package mx.bastekor.flowweaver.functional.microservices.payments_service.controller;

import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentRequest;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentResponse;
import mx.bastekor.flowweaver.functional.microservices.payments_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    public PaymentResponse pay(PaymentRequest request) {
        return paymentService.pay(request);
    }
}
