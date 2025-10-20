package mx.bastekor.flowweaver.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.dto.AuditTrailDTO;
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
            CURRENT_THREAD_CONTAINER.set(container);
            log.debug("🧵 ThreadContainer creado: {} | Thread: {}", container.getThreadId(), container.getThreadName());
        }
        return container;
    }

    /**
     * Limpiar ThreadContainer completo.
     */
    public static void clearCurrentThreadContainer() {
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        if (container != null) {
            log.debug("🧹 ThreadContainer limpiado: {} | BusinessLogs: {}",
                    container.getThreadId(), container.getBusinessLogs().size());
            CURRENT_THREAD_CONTAINER.remove();
        }
    }

    public static BusinessLogContainer assignBusinessLogContainer(String operationCode) {

        ThreadContainer threadContainer = CURRENT_THREAD_CONTAINER.get();

        BusinessLogContainer businessLogContainer;

        if (isBlank(operationCode)) {
            // Buscar el "default" BL# y si no se encuentra, generar uno nuevo con default BL#
            businessLogContainer = ofNullable(threadContainer.getBusinessLogContainerDefault())
                    .stream()
                    .peek(blc -> log.warn("👨 [PADRASTRO] Recuperando BusinessLog automático: [{}|{}] para AuditTrail huérfano.",
                            blc.getOperationCode(), blc.getFlowId()))
                    .findFirst()
                    .orElseGet(() -> {
                        BusinessLogContainer temp = new BusinessLogContainer(generate(BUSINESS_LOG_PREFIX));
                        addBusinessLogContainer(temp);
                        return temp;
                    });
        } else {
            // Si contiene operationCode "BusinessLog.operationCode" o "AuditTrail.flowCode"
            // Buscar BusinessLogContainer en el pool por su "operationCode", si no se encuentra creamos uno.
            businessLogContainer = ofNullable(threadContainer.getBusinessLogContainer(operationCode))
                    .orElseGet(() -> {
                        BusinessLogContainer temp = new BusinessLogContainer(generate(operationCode));
                        addBusinessLogContainer(temp);
                        return temp;
                    });

        }
        return businessLogContainer;
    }

    /**
     * Agregar BusinessLogContainer al thread actual
     */
    public static void addBusinessLogContainer(BusinessLogContainer businessLogContainer) {

        ThreadContainer threadContainer = getCurrentThreadContainer();
        log.info("📦 BusinessLog establecido en thread [{}]: [{}|{}]",
                Thread.currentThread().getName(),
                businessLogContainer.getOperationCode(),
                businessLogContainer.getFlowId());

        threadContainer.addBusinessLogContainer(businessLogContainer);

        log.info("💾 BusinessLog guardado: [{}|{}] con {} AuditTrails",
                businessLogContainer.getOperationCode(),
                businessLogContainer.getFlowId(),
                businessLogContainer.getAuditTrailCount());
    }

    /**
     * Limpiar BusinessLog específico del thread
     */
    public static void clearBusinessLogContainer(String operationCode) {
        ThreadContainer threadContainer = CURRENT_THREAD_CONTAINER.get();
        BusinessLogContainer businessLogContainer = threadContainer.getBusinessLogContainer(operationCode);
        if (businessLogContainer != null) {
            log.debug("🧹 BusinessLog limpiado del thread [{}]: [{}|{}]",
                    Thread.currentThread().getName(),
                    businessLogContainer.getOperationCode(),
                    businessLogContainer.getFlowId());
            threadContainer.clearBusinessLogContainer(operationCode);

            // Si ya no hay BusinessLogs, limpiar el ThreadContainer completo
            if (threadContainer.getBusinessLogs().isEmpty()) {
                clearCurrentThreadContainer();
            }
        }
    }
}