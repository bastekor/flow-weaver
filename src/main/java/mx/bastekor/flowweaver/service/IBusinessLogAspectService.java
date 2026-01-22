package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import org.aspectj.lang.ProceedingJoinPoint;

public interface IBusinessLogAspectService {

    void processBusinessLog(BusinessLogContainer businessLogContainer);

    void processBusinessLog(BusinessLog businessLog,
                            String snapshot,
                            StatusEnum statusEnum,
                            Object response,
                            Throwable exception,
                            BusinessLogContainer businessLogContainer);

    void processAuditTrailIn(AuditTrail auditTrail,
                             String snapshot,
                             AuditTrailContainer auditTrailContainer);

    void processAuditTrailOut(AuditTrail auditTrail,
                              String snapshot,
                              StatusEnum statusEnum,
                              Object response,
                              Throwable exception,
                              AuditTrailContainer auditTrailContainer);
}