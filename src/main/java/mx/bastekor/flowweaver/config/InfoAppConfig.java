package mx.bastekor.flowweaver.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@ToString
@Configuration
@ConfigurationProperties(prefix = "info.app")
public class InfoAppConfig {
    public String name;
    public String version;
    public String description;
}