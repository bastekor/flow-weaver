package mx.bastekor.flowweaver.functional.microservices.payments_service.gateway;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.functional.microservices.payments_service.dto.PaymentRequest;
import org.springframework.stereotype.Component;

@Component
public class NotificationGateway {

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "SEND-EMAIL",
            defaultDescription = "Enviando notificación por correo electrónico",
            defaultValue = "Correo enviado exitosamente"
    )
    public String sendEmail(PaymentRequest request) {
        return "EMAIL-" + request.getClientId();
    }

    @AuditTrail(
            parentCode = "EXECUTE-PAYMENT",
            code = "SEND-SMS",
            defaultDescription = "Enviando notificación por SMS",
            defaultValue = "SMS enviado exitosamente"
    )
    public String sendSMS(PaymentRequest request) {
        return "SMS-" + request.getClientId();
    }
}
