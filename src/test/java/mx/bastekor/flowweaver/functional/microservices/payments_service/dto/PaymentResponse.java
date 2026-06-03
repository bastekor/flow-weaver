package mx.bastekor.flowweaver.functional.microservices.payments_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {
    private String transactionId;
    private String status;
    private String message;
    private String authorizationCode;
    private Double amount;
}
