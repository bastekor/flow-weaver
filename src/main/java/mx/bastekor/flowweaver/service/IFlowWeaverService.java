package mx.bastekor.flowweaver.service;

import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.dto.SimpleRequestDTO;

import java.util.Map;

public interface IFlowWeaverService {

    Map<String, Object> generate(SimpleRequestDTO request);

    void trace(SimpleRequestDTO request);

    void trace(SimpleRequestDTO request, FrameConfig config);
}
