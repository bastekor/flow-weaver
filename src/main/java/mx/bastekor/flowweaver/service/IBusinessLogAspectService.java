package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.BusinessLogEvent;

public interface IBusinessLogAspectService {

    void processBusinessLog(BusinessLogEvent businessLogEvent, StatusEnum status, String flowId);
}