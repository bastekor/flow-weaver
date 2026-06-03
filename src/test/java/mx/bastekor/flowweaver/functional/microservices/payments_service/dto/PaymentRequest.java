package mx.bastekor.flowweaver.functional.microservices.payments_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import mx.bastekor.flowweaver.functional.microservices.payments_service.constant.PaymentConstants.BeneficiaryType;
import mx.bastekor.flowweaver.functional.microservices.payments_service.constant.PaymentConstants.PaymentOption;

@Data
@AllArgsConstructor
public class PaymentRequest {
    private BeneficiaryType beneficiaryType;
    private String clientId;
    private String sourceAccount;
    private String targetCard;
    private Double amount;
    private PaymentOption paymentOption;
    private String currency;
    private String serviceId;
}
