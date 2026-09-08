package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.context.AuditTrailContainer;
import mx.bastekor.flowweaver.context.BusinessLogContainer;

public interface IFlowWeaverAspectService {

    void processBusinessLog(BusinessLogContainer businessLogContainer);
    void processAuditTrail(AuditTrailContainer auditTrailContainer);
}