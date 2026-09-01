package mx.bastekor.flowweaver.handler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.config.FrameConfig;
import mx.bastekor.flowweaver.dto.RequestDTO;
import mx.bastekor.flowweaver.resolver.ResolutionError;
import mx.bastekor.flowweaver.util.FrameFormatter;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.LF;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
@AllArgsConstructor
public class LoggingFlowWeaverResultHandler implements FlowWeaverResultHandler {

    private final FrameConfig frameConfig;
    private final FrameFormatter frameFormatter;

    @Override
    public void handle(final RequestDTO requestDTO, final Map<String, Object> fields) {
        final String hashTag = "#".repeat(50);
        final String enDash = "-".repeat(50);
        final String frame = frameFormatter.format(fields, frameConfig);
        final String requestId = "%s|%s|%s".formatted(requestDTO.getId(), requestDTO.getGroup(), requestDTO.getCode());
        /*
        Result...
        ##################################################
        keyA|keyB|keyC
        valueA|valueB|valueC
        ##################################################
        Errors...
            - keyA = messageB
                * suggestion1
                * suggestionN
            - keyB = messageB
                * suggestion1
                * suggestionN
        ##################################################
         */

        StringBuilder sb = new StringBuilder();
        sb.append(LF).append(hashTag).append(LF).append(" - Flow Weaver Result").append("(").append(requestId).append(")").append(LF);
        sb.append(frame).append(LF);
        if (frameConfig.isShowErrors() && !isEmpty(requestDTO.getResolutions())) {
            sb.append(enDash).append(LF).append(" - Flow Weaver Errors").append("(").append(requestId).append(")").append(LF);
            requestDTO.getResolutions().entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().getError() != null)
                    .forEach(entry -> {
                        final ResolutionError error = entry.getValue().getError();
                        sb.append("\t * key=").append(entry.getKey()).append(", message=").append(error.getMessage()).append(LF);
                        error.getSuggestions().forEach(suggestion -> sb.append("\t\t - suggestion=").append(suggestion).append(LF));
                    });
            sb.append(hashTag);
        }
        sb.append(hashTag);
        log.info(sb.toString());
    }
}