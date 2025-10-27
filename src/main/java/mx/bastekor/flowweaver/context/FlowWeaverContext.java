package mx.bastekor.flowweaver.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.model.ThreadContainer;

import static java.util.Optional.ofNullable;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Contexto para mantener el BusinessLog actual en cada thread
 * Usa InheritableThreadLocal para propagar a threads hijos
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlowWeaverContext {

    private static final String BUSINESS_LOG_PREFIX = "BL#";
    private static final String AUDIT_TRAIL_PREFIX = "AT#";

    private static final InheritableThreadLocal<ThreadContainer> CURRENT_THREAD_CONTAINER =
            new InheritableThreadLocal<>();

    /**
     * Agregar {@link ThreadContainer} al contexto actual
     *
     * @param threadContainer Objeto que representa el hilo actual
     */
    public static void setCurrentThreadContainer(ThreadContainer threadContainer) {
        CURRENT_THREAD_CONTAINER.set(threadContainer);
        log.debug("🧵 ThreadContainer creado: {} | Thread: {}", threadContainer.getThreadId(), threadContainer.getThreadName());
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
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        if (container != null) {
            log.debug("🧹 ThreadContainer limpiado: [{}|{}] | Total de BusinessLogContainers: {}",
                    container.getThreadId(),
                    container.getThreadName(),
                    container.getAllBusinessLogContainer().size());
            CURRENT_THREAD_CONTAINER.remove();
        }
    }

    public static BusinessLogContainer assignBusinessLogContainer(String operationCode) {

        ThreadContainer threadContainer = getCurrentThreadContainer();

        BusinessLogContainer businessLogContainer;

        if (isBlank(operationCode)) {
            // Buscar el "default" BL# y si no se encuentra, generar uno nuevo con default BL#
            businessLogContainer = ofNullable(threadContainer.getBusinessLogContainerDefault())
                    .stream()
                    .peek(blc -> log.warn("👨 [PADRASTRO] Recuperando BusinessLog automático: [{}|{}] para AuditTrail huérfano.",
                            blc.getFlowId(), blc.getOperationCode()))
                    .findFirst()
                    .orElseGet(() -> createBusinessLogContainer(generate(BUSINESS_LOG_PREFIX)));
        } else {
            // Si contiene operationCode "BusinessLog.operationCode" o "AuditTrail.flowCode"
            // Buscar BusinessLogContainer en el pool por su "operationCode", si no se encuentra creamos uno.
            businessLogContainer = ofNullable(threadContainer.getBusinessLogContainer(operationCode))
                    .orElseGet(() -> createBusinessLogContainer(operationCode));

        }
        return businessLogContainer;
    }

    private static BusinessLogContainer createBusinessLogContainer(final String operationCode) {
        ThreadContainer threadContainer = getCurrentThreadContainer();
        final BusinessLogContainer businessLogContainer = new BusinessLogContainer(operationCode);
        log.debug("🏁 [BusinessLog START] [{}|{}] | Thread: [{}|{}]",
                businessLogContainer.getFlowId(),
                businessLogContainer.getOperationCode(),
                threadContainer.getThreadId(),
                threadContainer.getThreadName());
        addBusinessLogContainer(businessLogContainer);
        return businessLogContainer;
    }

    /**
     * Agregar BusinessLogContainer al thread actual
     */
    public static void addBusinessLogContainer(BusinessLogContainer businessLogContainer) {

        ThreadContainer threadContainer = getCurrentThreadContainer();
        log.info("📦 BusinessLog establecido en thread [{}|{}]: [{}|{}]",
                threadContainer.getThreadId(),
                threadContainer.getThreadName(),
                businessLogContainer.getFlowId(),
                businessLogContainer.getOperationCode());

        threadContainer.addBusinessLogContainer(businessLogContainer);

        log.info("💾 BusinessLog guardado: [{}|{}] con {} AuditTrails",
                businessLogContainer.getFlowId(),
                businessLogContainer.getOperationCode(),
                businessLogContainer.getAuditTrailCount());
    }

    /**
     * Limpiar BusinessLog específico del thread
     */
    public static void clearBusinessLogContainer(String operationCode) {
        ThreadContainer threadContainer = getCurrentThreadContainer();
        BusinessLogContainer businessLogContainer = threadContainer.getBusinessLogContainer(operationCode);
        if (businessLogContainer != null) {
            log.debug("🧹 BusinessLog limpiado del threadContainer [{}|{}]: [{}|{}]",
                    threadContainer.getThreadId(),
                    threadContainer.getThreadName(),
                    businessLogContainer.getOperationCode(),
                    businessLogContainer.getFlowId());
            threadContainer.clearBusinessLogContainer(operationCode);

            // Si ya no hay BusinessLogs, limpiar el ThreadContainer completo
            if (threadContainer.getAllBusinessLogContainer().isEmpty()) {
                clearCurrentThreadContainer();
            }

            log.debug("🏁 [BusinessLog END] [{}|{}] | Duration: {} | AuditTrails: {} | Thread: [{}|{}]",
                    businessLogContainer.getFlowId(),
                    businessLogContainer.getOperationCode(),
                    businessLogContainer.getDuration(),
                    businessLogContainer.getAuditTrails().size(),
                    threadContainer.getThreadId(),
                    threadContainer.getThreadName());
        }
    }

    public static void addAuditTrailContainer(int type,
                                              final AuditTrailContainer auditTrailContainer,
                                              final BusinessLogContainer businessLogContainer) {
        ThreadContainer threadContainer = getCurrentThreadContainer();
        if (type == 0) {
            log.debug("  ▶️ [AuditTrail ENTRADA] [{}|{}] | BusinessLog: [{}|{}] | Thread: [{}|{}]",
                    auditTrailContainer.getFlowId(),
                    auditTrailContainer.getOperationCode(),
                    businessLogContainer.getFlowId(),
                    businessLogContainer.getOperationCode(),
                    threadContainer.getThreadId(),
                    threadContainer.getThreadName());
        } else {
            log.debug("  ◀️ [AuditTrail SALIDA] [{}|{}] | BusinessLog: [{}|{}] | Thread: [{}|{}]",
                    auditTrailContainer.getFlowId(),
                    auditTrailContainer.getOperationCode(),
                    businessLogContainer.getFlowId(),
                    businessLogContainer.getOperationCode(),
                    threadContainer.getThreadId(),
                    threadContainer.getThreadName());
        }

        // Registrar el AuditTrail hijo de entrada en el BusinessLog padre
        businessLogContainer.addAuditTrail(auditTrailContainer);
    }

    public static boolean peekThreadContainerExists() {
        return CURRENT_THREAD_CONTAINER.get() != null;
    }

    /**
     * Imprime recursivamente el estado actual del contexto desde ThreadContainer hasta AuditTrail.
     * No crea contenedores nuevos si el contexto está vacío.
     */
    public static void printRecursive(boolean bool) {
        if (bool) {
            ThreadContainer tc = CURRENT_THREAD_CONTAINER.get();
            if (tc == null) {
                log.info("[FlowWeaver] Contexto vacío. No hay ThreadContainer actual.");
                return;
            }

            log.info("🧵 ThreadContainer [{}|{}] | BusinessLogs: {}",
                    tc.getThreadId(), tc.getThreadName(), tc.getAllBusinessLogContainer().size());

            for (BusinessLogContainer bl : tc.getAllBusinessLogContainer()) {
                log.info(" ├─ 📊 BusinessLogContainer [{}|{}] - [{}|{}] | AuditTrails: {} | Duration: {}",
                        tc.getThreadId(), tc.getThreadName(),
                        bl.getFlowId(), bl.getOperationCode(),
                        bl.getAuditTrailCount(), bl.getDuration());

                for (AuditTrailContainer at : bl.getAuditTrails()) {
                    log.info(" │   └─ 📝 AuditTrail [{}|{}] - [{}|{}] - [{}|{}] | flowCode: {} | Duration: {}",
                            tc.getThreadId(), tc.getThreadName(),
                            bl.getFlowId(), bl.getOperationCode(),
                            at.getFlowId(), at.getOperationCode(),
                            at.getFlowCode(), at.getDuration());
                }
            }
        }
    }
}