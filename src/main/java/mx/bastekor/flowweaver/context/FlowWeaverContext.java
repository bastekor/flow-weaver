package mx.bastekor.flowweaver.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.bastekor.flowweaver.model.BusinessLogContainer;
import mx.bastekor.flowweaver.model.ThreadContainer;

import java.util.Optional;

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
     * Obtener ThreadContainer actual (crea uno si no existe)
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
     * Limpiar ThreadContainer completo
     */
    public static void clearCurrentThread() {
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        if (container != null) {
            log.debug("🧹 ThreadContainer limpiado: {} | BusinessLogs: {}",
                    container.getThreadId(), container.getBusinessLogs().size());
        }
        CURRENT_THREAD_CONTAINER.remove();
    }

    /**
     * Agregar BusinessLogContainer al thread actual
     */
    public static void setBusinessLogContainerInCurrentThread(BusinessLogContainer businessLogContainer) {

        ThreadContainer container = getCurrentThreadContainer();
        log.info("📦 BusinessLog establecido en thread [{}]: [{}|{}]",
                Thread.currentThread().getName(),
                businessLogContainer.getOperationCode(),
                businessLogContainer.getFlowId());

        container.addBusinessLogContainer(businessLogContainer);

        log.info("💾 BusinessLog guardado: [{}|{}] con {} AuditTrails",
                businessLogContainer.getOperationCode(),
                businessLogContainer.getFlowId(),
                businessLogContainer.getAuditTrails().size());
    }

    /**
     * Obtener BusinessLogContainer por operationCode
     */
    public static BusinessLogContainer getBusinessLogContainerInCurrentThread(String operationCode) {
        return Optional.ofNullable(CURRENT_THREAD_CONTAINER.get())
                .map(threadContainer -> threadContainer.getBusinessLogContainer(operationCode))
                .orElse(null);
    }

    /**
     * ✅ NUEVO: Obtener el ÚNICO padrastro (BL#) del thread
     */
    public static BusinessLogContainer getPadrastroBusinessLog(String prefix) {
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        if (container == null) {
            return null;
        }

        // Buscar el BusinessLog que empieza con el prefix (BL#)
        return container.getBusinessLogs().stream()
                .filter(bl -> bl.getOperationCode().startsWith(prefix))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verificar si existe BusinessLog por operationCode
     */
    public static boolean existBusinessLogContainerInCurrentThread(String operationCode) {
        return operationCode != null &&
                CURRENT_THREAD_CONTAINER.get() != null &&
                CURRENT_THREAD_CONTAINER.get().getBusinessLogContainer(operationCode) != null;
    }

    /**
     * Limpiar BusinessLog específico del thread
     */
    public static void clearBusinessLogInCurrentThread(String operationCode) {
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        if (container != null && operationCode != null) {
            BusinessLogContainer businessLog = container.getBusinessLogContainer(operationCode);
            if (businessLog != null) {
                log.debug("🧹 BusinessLog limpiado del thread [{}]: [{}|{}]",
                        Thread.currentThread().getName(),
                        businessLog.getOperationCode(),
                        businessLog.getFlowId());
                container.clearBusinessLogContainer(operationCode);

                // Si ya no hay BusinessLogs, limpiar el ThreadContainer completo
                if (container.getBusinessLogs().isEmpty()) {
                    clearCurrentThread();
                }
            }
        }
    }

    /**
     * Obtener cantidad de BusinessLogs en el thread actual
     */
    public static int getSize() {
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        return container != null ? container.getBusinessLogs().size() : 0;
    }

    /**
     * ✅ NUEVO: Imprimir resumen del thread actual
     */
    public static void printThreadSummary() {
        ThreadContainer container = CURRENT_THREAD_CONTAINER.get();
        if (container == null) {
            log.info("📊 [THREAD SUMMARY] No hay ThreadContainer activo");
            return;
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   ThreadContainer Summary                              ║");
        log.info("╠════════════════════════════════════════════════════════╣");
        log.info("║ Thread ID: {}                                    ║", container.getThreadId());
        log.info("║ Thread Name: {}                              ║", container.getThreadName());
        log.info("║ BusinessLogs: {}                                       ║", container.getBusinessLogs().size());
        log.info("╠════════════════════════════════════════════════════════╣");

        container.getBusinessLogs().forEach(bl -> {
            log.info("║  📊 [{}|{}]", bl.getOperationCode(), bl.getFlowId());
            log.info("║     AuditTrails: {}                                  ║", bl.getAuditTrails().size());
        });

        log.info("╚════════════════════════════════════════════════════════╝");
    }

    public static void setCurrentThreadContainer(ThreadContainer threadContainer) {
        CURRENT_THREAD_CONTAINER.set(threadContainer);
    }
}