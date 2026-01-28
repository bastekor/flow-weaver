package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import org.aspectj.lang.ProceedingJoinPoint;

public interface IBusinessLogAspectService {

    void processBusinessLog(BusinessLogContainer businessLogContainer);
    void processAuditTrail(AuditTrailContainer auditTrailContainer);
}