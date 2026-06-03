package mx.bastekor.flowweaver.functional.microservices.payments_service.service;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "VALIDATE-CLIENT",
            defaultDescription = "Validando identidad del cliente",
            defaultValue = "Cliente validado correctamente"
    )
    public String validate(String clientId) {
        return clientId;
    }
}
