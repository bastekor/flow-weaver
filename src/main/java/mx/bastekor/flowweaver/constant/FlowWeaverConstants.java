package mx.bastekor.flowweaver.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlowWeaverConstants {
    public static final String GROUP_CODE_PREFIX = "GC#";
    public static final String BUSINESS_LOG_PREFIX = "BL#";
    public static final String AUDIT_TRAIL_PREFIX = "AT#";
    public static final String PROCESS_STATUS = "processStatus";
    public static final String REQUEST_ISNULL = "Object RequestDTO is null";
    public static final String FIELDS_IS_NULL_OR_EMPTY = "Object fields is null or empty";
}