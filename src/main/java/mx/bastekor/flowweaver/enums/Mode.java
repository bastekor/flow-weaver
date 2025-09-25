package mx.bastekor.flowweaver.enums;

import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * Enum que representa el modo de evaluación para la anotación {@link mx.bastekor.flowweaver.annotation.BusinessLog}.
 *
 * <ul>
 *   <li>{@link #STATIC}: Toma los valores directamente desde la anotación (código embebido).</li>
 *   <li>{@link #DYNAMIC}: Intenta resolver los valores desde una configuración externa (ej. application.yml).</li>
 *   <li>{@link #MERGED}: Fusiona ambos modos. Se prioriza la configuración externa, y si no existe, se usa el valor de la anotación.</li>
 * </ul>
 */
@ToString
@AllArgsConstructor
public enum Mode {

    /**
     * Modo completamente estático. Solo se toman los valores definidos en la anotación.
     */
    STATIC,

    /**
     * Modo dinámico. Se espera que los valores sean extraídos desde un archivo de configuración
     * (solo si es que existen).
     */
    DYNAMIC,

    /**
     * Modo combinado. Se mapean ambas entradas y se va evaluando el valor de cada una de las propiedades,
     * si no se resuelven con estáticas, entonces se toman las dinámicas y viceversa.
     */
    MERGED
}