package mx.bastekor.flowweaver.functional.microservices.payments_service.service;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "VERIFY-ACCOUNT",
            defaultDescription = "Verificando cuenta de origen",
            defaultValue = "Cuenta de origen verificada correctamente"
    )
    public String verify(String accountNumber) {
        return accountNumber;
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "VERIFY-BALANCE",
            defaultDescription = "Verificando saldo disponible",
            defaultValue = "Saldo suficiente verificado"
    )
    public Double verifyBalance(String accountNumber, Double amount) {
        return amount;
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "VERIFY-OWN-CARD",
            defaultDescription = "Verificando tarjeta propia",
            defaultValue = "Tarjeta propia verificada correctamente"
    )
    public String verifyOwnCard(String targetCard) {
        return targetCard;
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "VERIFY-THIRD-PARTY",
            defaultDescription = "Verificando cuenta de tercero",
            defaultValue = "Cuenta de tercero verificada correctamente"
    )
    public String verifyThirdParty(String targetCard) {
        return targetCard;
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "VERIFY-SERVICE",
            defaultDescription = "Verificando servicio a pagar",
            defaultValue = "Servicio verificado correctamente"
    )
    public String verifyService(String serviceId) {
        return serviceId;
    }
}
