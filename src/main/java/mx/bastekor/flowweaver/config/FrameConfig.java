package mx.bastekor.flowweaver.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.bastekor.flowweaver.enums.OutputMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "flow-weaver.frame")
public class FrameConfig {
    private OutputMode outputMode = OutputMode.DUAL_LINE;
    private String entrySeparator = "|";
    private String pairSeparator = "=";
    private boolean skipNulls = false;
    private boolean skipBlanks = false;
    private List<String> excludedKeys = new ArrayList<>();
}
