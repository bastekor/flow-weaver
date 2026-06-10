package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.dto.RequestDTO;

import java.util.Map;

public interface IFlowWeaverService {

    Map<String, Object> generate(RequestDTO request);

    void trace(RequestDTO request);

    void trace(RequestDTO request, FrameConfig config);
}
