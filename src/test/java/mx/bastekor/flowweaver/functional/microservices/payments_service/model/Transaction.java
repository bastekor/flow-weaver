package mx.bastekor.flowweaver.functional.microservices.payments_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String transactionId;
    private String sourceAccount;
    private String targetAccount;
    private Double amount;
    private String currency;
    private String type;
    private String status;
    private String authorizationCode;
    private LocalDateTime timestamp;
}
