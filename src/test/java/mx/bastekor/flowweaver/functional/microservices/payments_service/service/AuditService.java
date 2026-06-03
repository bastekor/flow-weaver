package mx.bastekor.flowweaver.functional.microservices.payments_service.service;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @AuditTrail(
            parentCode = "",
            code = "AUDIT-OPERATION",
            defaultDescription = "Registrando operación en auditoría",
            defaultValue = "Operación auditada correctamente"
    )
    public String audit(String operation, String detail) {
        return "AUDIT-" + operation + "-" + detail;
    }
}
