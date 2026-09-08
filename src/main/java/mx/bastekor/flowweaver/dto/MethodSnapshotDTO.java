package mx.bastekor.flowweaver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Representa la firma del método anotado, reconstruida a partir de los
 * snapshots (JSON string) que captura el interceptor en tiempo de ejecución.
 * <p>
 * Es un contenedor plano (DTO): la reconstrucción completa la realiza
 * {@code SafeSnapshotMapper#toMethodSignature(String, String, String)} a partir
 * de las "fotos" ({@code mapArgs} y {@code mapObject}) tomadas por
 * {@code SafeSnapshotMapper}, sin depender de los objetos en flujo de ejecución.
 * Todo su contenido es {@code null}-safe: si un dato no pudo reconstruirse
 * (ausente, truncado por {@code maxDepth}, clase no disponible, etc.) simplemente
 * queda en {@code null} sin romper el proceso.
 *
 * @param <T> tipo del objeto de respuesta del método anotado.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MethodSnapshotDTO<T> {
    private String className;
    private String methodName;
    private String returnType;
    private String methodDuration;
    private Map<String, Object> args;
    private T response;
    private boolean exception;
}