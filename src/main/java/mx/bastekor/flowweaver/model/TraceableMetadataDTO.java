package mx.bastekor.flowweaver.model;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class TraceableMetadataDTO extends LogSegment {
    private String uuid;
    private String timestamp;
    private String operationCode;
    private String operationDescription;
    private String status;
    private String response;
    private String appName;
    private String appDescription;
    private String appVersion;

    @Override
    public String toPipeString() {
        return String.join("|",
                defaultString(uuid),
                defaultString(timestamp),
                defaultString(operationCode),
                defaultString(operationDescription),
                defaultString(status),
                defaultString(response),
                defaultString(appName),
                defaultString(appDescription),
                defaultString(appVersion)
        );
    }
}
