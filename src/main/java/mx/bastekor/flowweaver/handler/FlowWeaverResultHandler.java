package mx.bastekor.flowweaver.handler;

import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.dto.RequestDTO;

import java.util.Map;

public interface FlowWeaverResultHandler {
    void handle(RequestDTO requestDTO, Map<String, Object> fields);
}