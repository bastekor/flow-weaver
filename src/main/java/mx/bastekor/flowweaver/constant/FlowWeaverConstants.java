package mx.bastekor.flowweaver.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlowWeaverConstants {

    public static final String FLOW_WEAVER_CONTEXT_ID = "flow-weaver-context-id";

    // BusinessLog Aspect Messages
    public static final String BUSINESS_LOG_START = "===== Start interceptor for BusinessLog ===== ";
    public static final String BUSINESS_LOG_END = "===== End interceptor for BusinessLog ({}) ===== ";
    public static final String BUSINESS_LOG_DEBUG_START = "s_BusinessLog ID :: [{}]";
    public static final String BUSINESS_LOG_DEBUG_END = "e_BusinessLog ID :: [{}|{}]";
    public static final String BUSINESS_LOG_ERROR = "BusinessLog error ID :: [{}|{}], MSG: {}";
    public static final String BUSINESS_LOG_RETRY_ERROR = "Failed to retry enqueue with {} status: {}";

    // AuditTrail Aspect Messages
    public static final String AUDIT_TRAIL_ENTRY = "AuditTrail - Entrada - {}";
    public static final String AUDIT_TRAIL_EXIT = "AuditTrail - Salida-{} - {}";
}