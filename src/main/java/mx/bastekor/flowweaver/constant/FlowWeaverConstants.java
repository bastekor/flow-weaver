package mx.bastekor.flowweaver.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlowWeaverConstants {

    public static final String FLOW_WEAVER_CONTEXT_ID = "flow-weaver-context-id";

    // BusinessLog Aspect Messages
    public static final String BUSINESS_LOG_START = "===== Start interceptor for BusinessLog ===== ";
    public static final String BUSINESS_LOG_END = "===== End interceptor for BusinessLog ({}) ===== ";
    public static final String BUSINESS_LOG_ERROR = "BusinessLog error ID :: [{}|{}], MSG: {}";
    public static final String BUSINESS_LOG_RETRY_ERROR = "Failed to retry enqueue with {} status: {}";

    // BusinessLog Aspect Messages
    public static final String AUDIT_TRAIL_START = "===== Start interceptor for AuditTrail ===== ";
    public static final String AUDIT_TRAIL_END = "===== End interceptor for AuditTrail ({}) ===== ";
    public static final String AUDIT_TRAIL_DEBUG_START = "s_AuditTrail ID :: [{}]";
    public static final String AUDIT_TRAIL_DEBUG_END = "e_AuditTrail ID :: [{}|{}]";
    public static final String AUDIT_TRAIL_ERROR = "AuditTrail error ID :: [{}|{}], MSG: {}";
    public static final String AUDIT_TRAIL_RETRY_ERROR = "Failed to retry enqueue with {} status: {}";
}