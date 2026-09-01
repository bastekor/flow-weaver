package mx.bastekor.flowweaver.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación utilizada para definir parámetros individuales que serán evaluados
 * como parte del registro de entrada/salida de un flujo funcional dentro de las anotaciones
 * {@link BusinessLog} y {@link AuditTrail}.
 * <p>
 * Esta anotación permite especificar claves y expresiones para extraer información
 * desde los argumentos del método interceptado, evaluando primero el valor como una expresión,
 * y en caso de no resolverse, utilizando un valor por defecto.
 * <p>
 * Esta anotación debe ser usada exclusivamente como parte de otra anotación ({@link BusinessLog#dataOut()},
 * {@link AuditTrail#dataIn()}, {@link AuditTrail#dataOut()}, {@link AuditTrail#dataInOut()}).
 *
 * <p><b>Reglas de resolución:</b></p>
 * <ul>
 *   <li>Si {@code value} contiene una expresión válida (por ejemplo: response.data.id o args[0].nombre), se intenta evaluarla.</li>
 *   <li>Si no puede resolverse, se utiliza el valor de {@code defaultValue} como respaldo.</li>
 *   <li>Si ambos son vacíos, el campo se ignora.</li>
 * </ul>
 *
 * <p><b>Ejemplo:</b></p>
 * <pre>
 * {@code
 * @BusinessLog(
 *   dataOut = {
 *     @DataParam(key = "userId", value = "response.user.id", defaultValue = "0"),
 *     @DataParam(key = "username", value = "args[0].username", defaultValue = "desconocido")
 *   }
 * )
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface DataParam {

    /**
     * Clave asociada al valor extraído. Esta clave se usará como nombre
     * del campo en el DTO final que será almacenado o auditado.
     *
     * @return clave del parámetro
     */
    String key();

    /**
     * Expresión que define el valor a extraer desde los argumentos del método.
     * Si la expresión no puede resolverse, se utilizará {@link #defaultValue()}.
     * <p>
     * Ejemplo: "args[0].nombre", "cliente.id", etc.
     *
     * @return expresión para extracción de datos
     */
    String value() default "";

    /**
     * Valor por defecto en caso de que {@link #value()} no pueda resolverse
     * dinámicamente desde los argumentos del método.
     *
     * @return valor por defecto del parámetro
     */
    String defaultValue() default "";
}