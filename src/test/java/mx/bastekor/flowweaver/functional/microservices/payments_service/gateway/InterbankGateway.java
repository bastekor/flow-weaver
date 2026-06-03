package mx.bastekor.flowweaver.functional.microservices.payments_service.gateway;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import org.springframework.stereotype.Component;

@Component
public class InterbankGateway {

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "VALIDATE-INTERBANK",
            defaultDescription = "Validando cuenta destino en banco externo",
            defaultValue = "Cuenta interbancaria validada correctamente"
    )
    public String validate(String targetCard) {
        return targetCard;
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "TRANSFER-INTERBANK",
            defaultDescription = "Ejecutando transferencia interbancaria",
            defaultValue = "Transferencia interbancaria exitosa"
    )
    public String transfer(String targetCard, Double amount) {
        return "INTERBANK-" + targetCard;
    }
}
