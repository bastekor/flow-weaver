package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.annotation.AuditTrail;
import mx.bastekor.flowweaver.annotation.BusinessLog;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import org.aspectj.lang.ProceedingJoinPoint;

public interface IBusinessLogAspectService {

//    void processBusinessLog(BusinessLog businessLog,
//                            ProceedingJoinPoint joinPoint,
//                            StatusEnum statusEnum,
//                            Object response,
//                            Throwable exception,
//                            BusinessLogContainer businessLogContainer);

    void processBusinessLog(BusinessLog businessLog,
                            String snapshot,
                            StatusEnum statusEnum,
                            Object response,
                            Throwable exception,
                            BusinessLogContainer businessLogContainer);

    void processAuditTrailIn(AuditTrail auditTrail,
                             ProceedingJoinPoint joinPoint,
                             AuditTrailContainer auditTrailContainer);

    void processAuditTrailOut(AuditTrail auditTrail,
                              ProceedingJoinPoint joinPoint,
                              StatusEnum statusEnum,
                              Object response,
                              Throwable exception,
                              AuditTrailContainer auditTrailContainer);
}