package mx.bastekor.flowweaver.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Setter
@Getter
@ToString
@Configuration
@ConfigurationProperties(prefix = "flow-weaver")
public class FlowWeaverRootConfig {
    private Map<String, BusinessLogDTO> businessLogs;
    private Map<String, AuditTrailDTO> auditTrails;
}