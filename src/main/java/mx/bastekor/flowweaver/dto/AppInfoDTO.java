package mx.bastekor.flowweaver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AppInfoDTO {
    private String appName;
    private String appVersion;
    private String appDescription;
    private String hostName;
    private String ipAddress;
    private String instanceId;
    private String region;
    private String zone;
}