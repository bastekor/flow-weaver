package mx.bastekor.flowweaver.annotation;

import mx.bastekor.flowweaver.enums.Mode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para registrar información de auditoría técnica o funcional
 * (audit trail) durante la ejecución de un método.
 *
 * <p>
 * Permite declarar metadatos estáticos o dinámicos relacionados con el flujo funcional,
 * tales como código de operación, descripción, resultado del método, excepciones
 * y datos personalizados de entrada/salida.
 * </p>
 *
 * <p>
 * Está diseñada para usarse junto con mecanismos de interceptación como AOP
 * o aspectos personalizados que gestionen trazabilidad y monitoreo.
 * </p>
 *
 * <p><b>Expresiones y valores por defecto:</b></p>
 * <ul>
 *   <li>Las propiedades como {@code value}, {@code exception}, {@code description}, etc.,
 *   pueden contener expresiones que se evaluarán contra el contexto del método.</li>
 *   <li>Si no se pueden resolver en tiempo de ejecución, se utilizarán los valores de respaldo:
 *   {@code defaultValue}, {@code defaultException}, {@code defaultDescription}, etc.</li>
 *   <li>Si ambos están vacíos, el campo será omitido del registro.</li>
 * </ul>
 *
 * <p><b>Modos de operación:</b> definidos mediante {@link Mode}</p>
 * <ul>
 *   <li>{@code STATIC} (por defecto): Usa los valores definidos directamente.</li>
 *   <li>{@code DYNAMIC}: Evalúa expresiones contra los argumentos o el resultado.</li>
 *   <li>{@code MERGED}: Combinación de ambos enfoques (el dinámico tiene prioridad).</li>
 * </ul>
 *
 * <p><b>Relación con {@code @BusinessLog}:</b></p>
 * Puede vincularse a una anotación {@code @BusinessLog} mediante el atributo
 * {@code relatedBusinessCode}, facilitando el rastreo unificado de un flujo completo.
 *
 * <p><b>Asignación automática de operación padre:</b></p>
 * Si no se asigna el valor de `relateBusinessLog`, se generará uno por defecto usando la convención:
 * <pre>
 *   {application-name}_class_{className}#methodName
 * </pre>
 * Por ejemplo, si la aplicación se llama {@code payments-service} y el método anotado es
 * {@code miMetodo()} dentro de {@code AService}, entonces se generará:
 * <pre>
 *   payments-service_class_AService#miMetodo
 * </pre>
 * Esto garantiza que todos los eventos tengan al menos un identificador de flujo funcional padre,
 * incluso si no se definió explícitamente un {@code @BusinessLog}.
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>{@code
 * @AuditTrail(
 *   relatedBusinessCode = "CLIENT-CREATE",
 *   operationCode = "client.trx.payId",
 *   description = "client.trx.payDesc",
 *   defaultDescription = "Se audita la creación del cliente",
 *   value = "response.status",
 *   defaultValue = "OK",
 *   exception = "exception.message",
 *   defaultException = "Error inesperado",
 *   dataIn = {
 *     @DataParam(key = "requestNombre", value = "args[0].nombre", defaultValue = "Desconocido")
 *   },
 *   dataOut = {
 *     @DataParam(key = "clientId", value = "response.data.id", defaultValue = "N/A")
 *   }
 * )
 * }</pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditTrail {

    /**
     * Código grupal de flujo funcional (groupCode) proporcionado por negocio.
     * Si no se especifica o no se resuelve, el sistema puede inferir un identificador
     * por default (GC#XXXX-XXXX).
     */
    String groupCode() default "";

    /**
     * Código de operación relacionado del contexto {@code @BusinessLog}, si aplica.
     * Permite establecer trazabilidad cruzada entre ambos mecanismos.
     */
    String flowCode() default "";

    /**
     * Código del flujo funcional (operationCode) proporcionado por negocio.
     * <p>
     * Puede ser:
     * - Una expresión que se evalúa desde la configuración externa (ligada al Custom Dev Code).
     * - Un texto directo definido aquí.
     * <p>
     * Si no se especifica o no se resuelve, el sistema puede inferir un identificador
     * por default (AT#XXXX-XXXX).
     */
    String operationCode() default "";

    /**
     * Descripción del flujo funcional.
     * <p>
     * Se intenta resolver como expresión contra la configuración o metadata.
     * Si falla, se toma <code>defaultDescription</code> como valor por defecto.
     */
    String description() default "";

    /**
     * Descripción por defecto usada si no se puede resolver <code>description</code>.
     * <p>
     * Siempre se trata como texto plano.
     */
    String defaultDescription() default "";

    /**
     * Expresión que representa el resultado del método exitoso.
     * <p>
     * Se evalúa contra el objeto de retorno del método (<code>response</code>).
     * Si no se resuelve correctamente, se toma <code>defaultValue</code>.
     * <p>
     * Ejemplo:
     * - value = "status.code" → buscará en response.getStatus().getCode()
     * - value = "message" → buscará response.getMessage()
     */
    String value() default "";

    /**
     * Valor por defecto del resultado si no se resuelve <code>value</code>.
     * <p>
     * Siempre se trata como texto plano.
     */
    String defaultValue() default "";

    /**
     * Expresión que representa el valor a obtener en caso de excepción.
     * <p>
     * Se evalúa contra la excepción lanzada (<code>Throwable</code>).
     * Si no se resuelve correctamente, se toma <code>defaultException</code>.
     * <p>
     * Ejemplo:
     * - exception = "error.code"
     * - exception = "message"
     */
    String exception() default "";

    /**
     * Valor por defecto en caso de excepción si no se resuelve <code>exception</code>.
     * <p>
     * Siempre se trata como texto plano.
     */
    String defaultException() default "";

    /**
     * Modo de operación de la anotación.
     * <p>
     * Determina si se trata de un log estático (sin evaluación de expresiones)
     * o uno dinámico (con extracción de datos en tiempo de ejecución).
     * <p>
     * Opciones:
     * <lo>
     * <li><b>STATIC (default)</b>: Se usa lo definido tal cual en la anotación o configuración.</li>
     * <li><b>DYNAMIC</b>: Se permite extracción en tiempo de ejecución desde los argumentos y el resultado
     * mediante datos de configuración.</li>
     * <li><b>MERGED</b>: Combina ambos enfoques (usualmente se da preferencia al dinámico si se resuelve).</li>
     * </lo>
     */
    Mode mode() default Mode.STATIC;

    /**
     * Datos personalizados de entrada del método.
     * <p>
     * Cada entrada representa un parámetro que se desea registrar en la pista de auditoria,
     * y puede contener expresiones para extraer información de los datos de entrada.
     * <p>
     * La clave será el nombre del parámetro registrado, y el valor será el resultado
     * de evaluar la expresión o, si falla, el valor por defecto.
     */
    DataParam[] dataIn() default {};

    /**
     * Datos personalizados de salida del método.
     * <p>
     * Cada entrada representa un parámetro que se desea registrar en la pista de auditoria,
     * y puede contener expresiones para extraer información del resultado (response), parámetros
     * del método o tomados de la configuración manual.
     * <p>
     * La clave será el nombre del parámetro registrado, y el valor será el resultado
     * de evaluar la expresión o, si falla, el valor por defecto.
     */
    DataParam[] dataOut() default {};

    /**
     * Datos personalizados de entrada y salida del método (para ambos casos son los mismos).
     * <p>
     * Cada entrada representa un parámetro que se desea registrar en la pista de auditoria,
     * y puede contener expresiones para extraer información del resultado (response), parámetros
     * del método o tomados de la configuración manual.
     * <p>
     * La clave será el nombre del parámetro registrado, y el valor será el resultado
     * de evaluar la expresión o, si falla, el valor por defecto.
     */
    DataParam[] dataInOut() default {};
}