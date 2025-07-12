package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.model.BusinessLogEvent;

public interface IBusinessLogAspectService {
    void enqueue(BusinessLogEvent businessLogEvent);
}