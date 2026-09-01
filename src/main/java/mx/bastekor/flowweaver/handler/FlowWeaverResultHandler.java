package mx.bastekor.flowweaver.handler;

import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.exception.FlowWeaverException;

import java.util.Map;

public interface FlowWeaverResultHandler {
    void handle(RequestDTO requestDTO, Map<String, Object> fields) throws FlowWeaverException;
}