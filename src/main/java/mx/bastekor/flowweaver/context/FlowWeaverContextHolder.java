package mx.bastekor.flowweaver.context;

import java.util.UUID;

/**
 * Clase para manejar el contexto de flujo (FlowWeaverContext) utilizando un ThreadLocal.
 * Permite inicializar, reutilizar y liberar el contexto en el mismo hilo,
 * asegurando que cada petición o flujo tenga su propio contexto aislado.
 * <p>
 * El contexto mantiene un contador de referencias para saber cuándo debe limpiarse.
 */
public class FlowWeaverContextHolder {

    /**
     * ThreadLocal que almacena el contexto específico del hilo actual.
     */
    private static final ThreadLocal<FlowWeaverContext> context = new ThreadLocal<>();

    /**
     * Inicializa un nuevo contexto si no existe uno para el hilo actual,
     * o reutiliza el contexto existente incrementando su contador de referencias.
     * <p>
     * Este método genera un nuevo ID de flujo si crea un contexto nuevo,
     * o reutiliza el ID del contexto existente si ya había uno.
     */
    public static void initOrReuse() {
        boolean created = get() == null;
        if (created) {
            String flowId = UUID.randomUUID().toString();
            initOrReuse(flowId);
        } else {
            initOrReuse(get().getFlowId());
        }
    }

    /**
     * Método privado que inicializa el contexto con un ID específico o
     * incrementa el contador de referencias si el contexto ya existe.
     *
     * @param flowId Identificador único para el contexto de flujo.
     */
    private static void initOrReuse(String flowId) {
        FlowWeaverContext ctx = context.get();
        if (ctx == null) {
            context.set(new FlowWeaverContext(flowId));
        } else {
            ctx.incrementRef();
        }
    }

    /**
     * Obtiene el contexto del flujo asociado al hilo actual.
     *
     * @return El {@link FlowWeaverContext} del hilo actual o null si no existe.
     */
    public static FlowWeaverContext get() {
        return context.get();
    }

    /**
     * Obtiene el identificador del flujo asociado al hilo actual.
     *
     * @return El {@link String} del hilo actual o null si no existe.
     */
    public static String getFlowId() {
        if (get() != null) {
            return get().getFlowId();
        }
        return null;
    }

    /**
     * Libera una referencia al contexto actual.
     * Si el contador de referencias llega a cero, limpia el contexto del hilo.
     * <p>
     * Este método debe llamarse al finalizar el uso del contexto en el flujo,
     * para evitar fugas de memoria en ThreadLocal.
     */
    public static void release() {
        FlowWeaverContext ctx = get();
        if (ctx != null) {
            ctx.decrementRef();
            if (ctx.getRefCount() <= 0) {
                context.remove();
            }
        }
    }

    /**
     * Limpia completamente el contexto asociado al hilo actual,
     * removiéndolo del ThreadLocal sin importar el contador de referencias.
     * <p>
     * Se debe usar al finalizar el flujo raíz para liberar recursos.
     */
    public static void clear() {
        context.remove();
    }
}