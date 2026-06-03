package mx.bastekor.flowweaver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.util.FrameExtractor;
import mx.bastekor.flowweaver.util.FrameFormatter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowWeaverService implements IFlowWeaverService {

    private final FrameExtractor frameExtractor;
    private final FrameFormatter frameFormatter;
    private final FrameConfig frameConfig;

    @Override
    public Map<String, Object> generate(RequestDTO request) {
        return frameExtractor.extract(request);
    }

    // Este al parecer no va a servir porque no tiene sentido si es que vamos a devolver
    // todos los valores con llave/valor
    @Override
    public Map<String, Object> generate(RequestDTO request, FrameConfig config) {
        return frameExtractor.extract(request);
    }

    @Override
    public void trace(RequestDTO request) {
        this.trace(request, this.frameConfig);
    }

    @Override
    public void trace(RequestDTO request, FrameConfig config) {
        Map<String, Object> fields = generate(request);
        String frame = frameFormatter.format(fields, config);
        log.info("\n{}", frame);
    }
}
