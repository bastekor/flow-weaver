package mx.bastekor.flowweaver.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.AUDIT_TRAIL_PREFIX;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_PREFIX;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.GROUP_CODE_PREFIX;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Contexto para mantener el BusinessLog actual en cada thread
 * Usa InheritableThreadLocal para propagar a threads hijos
 */
//@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlowWeaverContext {

    private static final InheritableThreadLocal<ThreadContainer> CURRENT_THREAD_CONTAINER =
            new InheritableThreadLocal<>();

    /**
     * Agregar {@link ThreadContainer} al contexto actual
     *
     * @param threadContainer Objeto que representa el hilo actual
     */
    public static void setCurrentThreadContainer(final ThreadContainer threadContainer) {
        CURRENT_THREAD_CONTAINER.set(threadContainer);
    }

    /**
     * Obtener ThreadContainer actual (crea uno si no existe)
     *
     * @return {@link ThreadContainer} actual.
     */
    public static ThreadContainer getCurrentThreadContainer() {
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        if (container == null) {
            container = new ThreadContainer();
            setCurrentThreadContainer(container);
        }
        return container;
    }

    /**
     * Limpiar ThreadContainer completo.
     */
    public static void clearCurrentThreadContainer() {
        ThreadContainer threadContainer = CURRENT_THREAD_CONTAINER.get();
        if (threadContainer != null) {
            CURRENT_THREAD_CONTAINER.remove();
        }
    }

    public static BusinessLogContainer assignBusinessLogContainer(final String entryGroup, final String entryCode) {

        ThreadContainer threadContainer = getCurrentThreadContainer();

        final String group = defaultIfBlank(entryGroup, generate(GROUP_CODE_PREFIX));
        final String code = defaultIfBlank(entryCode, generate(BUSINESS_LOG_PREFIX));

        if (code.startsWith(BUSINESS_LOG_PREFIX)) {
            // Buscar el "default" BL# y si no se encuentra, generar uno nuevo con default BL#
            return threadContainer.getBusinessLogContainerDefault()
                    .orElseGet(() -> createBusinessLogContainer(group, code));
        }
        // Si contiene code "BusinessLog.code" o "AuditTrail.flowCode"
        // Buscar BusinessLogContainer en el pool por su "code", si no se encuentra creamos uno.
        return threadContainer.getBusinessLogContainer(code)
                .orElseGet(() -> createBusinessLogContainer(group, code));
    }

    private static BusinessLogContainer createBusinessLogContainer(final String group, final String code) {
        final String correlationId = getCurrentThreadContainer().getCorrelationId();
        final BusinessLogContainer businessLogContainer = new BusinessLogContainer(correlationId, group, code);
        getCurrentThreadContainer().addBusinessLogContainer(businessLogContainer);
        return businessLogContainer;
    }

    public static AuditTrailContainer createAuditTrailContainer(final BusinessLogContainer businessLogContainer,
                                                                final String entryCode) {
        final String code = defaultIfBlank(entryCode, generate(AUDIT_TRAIL_PREFIX));
        final AuditTrailContainer auditTrailContainer = new AuditTrailContainer();
        auditTrailContainer.fillInFields(businessLogContainer.getGroup(), businessLogContainer.getCode(),
                businessLogContainer.getCorrelationId(), code);
        return auditTrailContainer;
    }

    /**
     * @param code Código del BusinessLog que se desea limpiar.
     *             Limpiar BusinessLog específico del thread
     */
    public static void clearBusinessLogContainer(final String code) {
        ThreadContainer threadContainer = getCurrentThreadContainer();
        threadContainer.getBusinessLogContainer(code)
                .ifPresent(businessLogContainer -> {
                    threadContainer.clearBusinessLogContainer(code);
                    if (threadContainer.getAllBusinessLogContainer().isEmpty()) {
                        clearCurrentThreadContainer();
                    }
                });
    }

    public static boolean peekThreadContainerExists() {
        return CURRENT_THREAD_CONTAINER.get() != null;
    }
}