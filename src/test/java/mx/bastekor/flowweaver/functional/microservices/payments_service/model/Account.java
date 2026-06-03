package mx.bastekor.flowweaver.functional.microservices.payments_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private String accountNumber;
    private String holderName;
    private String holderId;
    private Double balance;
    private String currency;
    private String type;
}
