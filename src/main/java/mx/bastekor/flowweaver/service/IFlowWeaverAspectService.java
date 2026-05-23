package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;

public interface IFlowWeaverAspectService {

    void processBusinessLog(BusinessLogContainer businessLogContainer);
    void processAuditTrail(AuditTrailContainer auditTrailContainer);
}