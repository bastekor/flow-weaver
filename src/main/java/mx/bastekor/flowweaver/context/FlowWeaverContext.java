package mx.bastekor.flowweaver.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.model.AuditTrailContainer;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.model.ThreadContainer;

import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.BUSINESS_LOG_PREFIX;
import static mx.bastekor.flowweaver.constant.FlowWeaverConstants.GROUP_CODE_PREFIX;
import static mx.bastekor.flowweaver.util.CodeGenerator.generate;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Contexto para mantener el BusinessLog actual en cada thread
 * Usa InheritableThreadLocal para propagar a threads hijos
 */
@Slf4j
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
        log.debug("🧵 [ThreadContainer START] [ThreadId: {}|ThreadName: {}]", threadContainer.getId(), threadContainer.getName());
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
            log.debug("🧵 [ThreadContainer END] [ThreadId: {}|ThreadName: {}]", threadContainer.getId(), threadContainer.getName());
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
                    .map(blc -> {
                        log.warn("👨 [PADRASTRO] Recuperando BusinessLog automático: [{}|{}] para AuditTrail huérfano.",
                                blc.getId(), blc.getCode());
                        return blc;
                    })
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
        logBusinessLogContainer(businessLogContainer, true);
        return businessLogContainer;
    }

    /**
     * @param code Código del BusinessLog que se desea limpiar.
     * Limpiar BusinessLog específico del thread
     */
    public static void clearBusinessLogContainer(final String code) {
        ThreadContainer threadContainer = getCurrentThreadContainer();
        threadContainer.getBusinessLogContainer(code)
                .ifPresent(businessLogContainer -> {
                    logBusinessLogContainer(businessLogContainer, false);
                    threadContainer.clearBusinessLogContainer(code);
                    if (threadContainer.getAllBusinessLogContainer().isEmpty()) {
                        clearCurrentThreadContainer();
                    }
                });
    }

    public static void addAuditTrailContainer(boolean isIncoming,
                                              final AuditTrailContainer auditTrailContainer,
                                              final BusinessLogContainer businessLogContainer) {
        businessLogContainer.addAuditTrail(auditTrailContainer);
        logAuditTrailContainer(businessLogContainer, auditTrailContainer, isIncoming);
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

            // Mejorar esto quizás leyendo las lineas por longitud para crear esto...
            log.info("🧵 ThreadContainer [{}|{}] | Total BusinessLogContainer: {} | Total AuditTrailContainers: {}",
                    tc.getId(), tc.getName(),
                    tc.getAllBusinessLogContainer().size(),
                    tc.getAllBusinessLogContainer().stream().mapToInt(BusinessLogContainer::getAuditTrailCount).sum());

            for (BusinessLogContainer bl : tc.getAllBusinessLogContainer()) {
                log.info(" ├─ 📊 BusinessLogContainer [{}|{}] - [{}|{}|{}] | AuditTrails: {} | Duration: {}",
                        tc.getId(), tc.getName(),
                        bl.getId(), bl.getGroup(), bl.getCode(),
                        bl.getAuditTrailCount(), bl.getDuration());

                for (AuditTrailContainer at : bl.getAuditTrails()) {
                    log.info(" │   └─ 📝 AuditTrail [{}|{}] - [{}|{}|{}] - [{}|{}] | flowCode: {} | Duration: {}",
                            tc.getId(), tc.getName(),
                            bl.getId(), bl.getGroup(), bl.getCode(),
                            at.getCorrelationId(), at.getCode(),
                            at.getParentCode(), at.getDuration());
                }
            }
        }
    }

    private static void logBusinessLogContainer(final BusinessLogContainer businessLogContainer,
                                                boolean isIncoming) {

        ThreadContainer threadContainer = getCurrentThreadContainer();
        if (isIncoming) {
            log.debug("🏁 [BusinessLog START] [{}|{}] | Thread: [{}|{}]",
                    businessLogContainer.getId(),
                    businessLogContainer.getCode(),
                    threadContainer.getId(),
                    threadContainer.getName());
        } else {
            log.debug("🧹 BusinessLog limpiado del threadContainer [{}|{}]: [{}|{}]",
                    threadContainer.getId(),
                    threadContainer.getName(),
                    businessLogContainer.getCode(),
                    businessLogContainer.getId());
            log.debug("🏁 [BusinessLog END] [{}|{}] | Duration: {} | AuditTrails: {} | Thread: [{}|{}]",
                    businessLogContainer.getId(),
                    businessLogContainer.getCode(),
                    businessLogContainer.getDuration(),
                    businessLogContainer.getAuditTrails().size(),
                    threadContainer.getId(),
                    threadContainer.getName());
        }
    }

    private static void logAuditTrailContainer(final BusinessLogContainer businessLogContainer,
                                               final AuditTrailContainer auditTrailContainer,
                                               boolean isIncoming) {

        ThreadContainer threadContainer = getCurrentThreadContainer();
        if (isIncoming) {
            log.debug("  ▶️ [AuditTrail ENTRADA] [{}|{}] | BusinessLog: [{}|{}] | Thread: [{}|{}]",
                    auditTrailContainer.getCorrelationId(),
                    auditTrailContainer.getCode(),
                    businessLogContainer.getId(),
                    businessLogContainer.getCode(),
                    threadContainer.getId(),
                    threadContainer.getName());
        } else {
            log.debug("  ◀️ [AuditTrail SALIDA] [{}|{}] | BusinessLog: [{}|{}] | Thread: [{}|{}]",
                    auditTrailContainer.getCorrelationId(),
                    auditTrailContainer.getCode(),
                    businessLogContainer.getId(),
                    businessLogContainer.getCode(),
                    threadContainer.getId(),
                    threadContainer.getName());
        }
    }
}