/**
 * Estado de ejecución del framework: objetos mutables con comportamiento
 * que viven mientras dura el contexto (por hilo/operación).
 * <p>
 * {@link mx.bastekor.flowweaver.context.FlowWeaverContext} es el holder
 * (ThreadLocal) que gestiona estos objetos; los {@code *Container}
 * representan el estado que se va llenando en runtime.
 * <p>
 * Lo que NO va aquí: portadores de datos sin comportamiento → {@code dto};
 * helpers estáticos sin estado → {@code util}.
 */
package mx.bastekor.flowweaver.context;