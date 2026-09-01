package mx.bastekor.flowweaver.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import mx.bastekor.flowweaver.enums.Mode;

/**
 * Anotación para registrar información de trazabilidad funcional (bitácora de negocio) durante la ejecución de un método.
 *
 * <p>
 * Permite declarar metadatos de forma estática o dinámica sobre la operación actual, incluyendo código de operación,
 * descripción, resultado del método y datos de salida personalizados.
 * Esta anotación está diseñada para ser utilizada junto con mecanismos de interceptación como AOP.
 * </p>
 *
 * <p>
 * Las propiedades principales pueden contener expresiones dinámicas. Si una expresión no se resuelve correctamente en
 * tiempo de ejecución, se utilizará el valor de respaldo correspondiente <b>(defaultDescription, defaultValue, etc.)</b>.
 * </p>
 *
 * <p><b>Convenciones de evaluación:</b></p>
 * <ul>
 *   <li>Una propiedad se considera una <b>expresión</b> si contiene un valor no vacío.</li>
 *   <li>Se intentará evaluar dicha expresión contra el contexto del método (por ejemplo: <code>args</code>,
 *   <code>response</code>, <code>exception</code>).</li>
 *   <li>Si la evaluación falla, se usará el valor por defecto definido en la propiedad <code>defaultX</code>.</li>
 *   <li>Si ambas están vacías, la propiedad será ignorada (a menos que se asigne valor por objeto de forma manual).</li>
 * </ul>
 *
 * <p>
 * El comportamiento general de la anotación se controla mediante el atributo <code>mode</code>, cuyo valor por defecto
 * es <code>STATIC</code>.
 * </p>
 *
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>{@code
 * @BusinessLog(
 *   code = "CLIENT-CREATE",
 *   description = "args[1].description",
 *   defaultDescription = "Se creó un nuevo cliente",
 *   value = "response.status",
 *   defaultValue = "200 OK",
 *   exception = "exception.message",
 *   defaultException = "Error inesperado",
 *   dataOut = {
 *     @DataParam(key = "clientId", value = "response.data.id", defaultValue = "9999-9999-9999-9999"),
 *     @DataParam(key = "clientName", value = "args[0].nombre", defaultValue = "Interno")
 *   }
 * )
 * public ClienteResponse crearCliente(ClienteRequest request) {
 *     // Lógica de creación
 * }
 * }</pre>
 */

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessLog {

    /**
     * Código grupal de flujo funcional (group) proporcionado por negocio.
     * Si no se especifica o no se resuelve, el sistema puede inferir un identificador
     * por default (GC#XXXX-XXXX).
     */
    String group() default "";

    /**
     * Código del flujo funcional (operationCode) proporcionado por negocio.
     * <p>
     * Puede ser:
     * - Una expresión que se evalúa desde la configuración externa (ligada al Custom Dev Code).
     * - Un texto directo definido aquí.
     * <p>
     * Si no se especifica o no se resuelve, el sistema puede inferir un identificador
     * por default (BL#XXXX-XXXX).
     */
    String code() default "";

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
     * <ul>
     * <li><b>STATIC (default)</b>: Se usa lo definido tal cual en la anotación o configuración.</li>
     * <li><b>DYNAMIC</b>: Se permite extracción en tiempo de ejecución desde los argumentos y el resultado
     * mediante datos de configuración.</li>
     * <li><b>MERGED</b>: Combina ambos enfoques (usualmente se da preferencia al dinámico si se resuelve).</li>
     * </ul>
     */
    Mode mode() default Mode.STATIC;

    /**
     * Datos personalizados de salida del método.
     * <p>
     * Cada entrada representa un parámetro que se desea registrar en la bitácora,
     * y puede contener expresiones para extraer información del resultado (response).
     * <p>
     * La clave será el nombre del parámetro registrado, y el valor será el resultado
     * de evaluar la expresión o, si falla, el valor por defecto.
     */
    DataParam[] dataOut() default {};
}