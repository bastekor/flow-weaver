package mx.bastekor.flowweaver.enums;

import lombok.AllArgsConstructor;

/**
 * Enum que representa los posibles estados de un flujo de negocio.
 * <ul>
 *   <li><b>SOURCE_SUCCESS</b>: El flujo de negocio se completó exitosamente.</li>
 *   <li><b>SOURCE_FAILURE</b>: El flujo de negocio no se completó correctamente debido a una condición esperada o controlada.</li>
 *   <li><b>EXCEPTION</b>: Se produjo una excepción lanzada por la librería durante la ejecución del flujo.</li>
 *   <li><b>ERROR</b>: Ocurrió un error no contemplado o inesperado dentro de la librería.</li>
 * </ul>
 */
@AllArgsConstructor
public enum StatusEnum {
    /**
     * El flujo de negocio se completó exitosamente.
     */
    SOURCE_SUCCESS,

    /**
     * El flujo de negocio no se completó correctamente debido a una condición esperada o controlada.
     */
    SOURCE_FAILURE,

    /**
     * Se produjo una excepción lanzada por la librería durante la ejecución del flujo.
     */
    EXCEPTION,

    /**
     * Ocurrió un error no contemplado o inesperado dentro de la librería.
     */
    ERROR,

    INTERNAL_SUCCESS, INTERNAL_FAULT, INTERNAL_ERROR
}