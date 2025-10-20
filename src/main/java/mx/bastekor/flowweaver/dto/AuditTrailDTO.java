package mx.bastekor.flowweaver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.enums.Mode;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class AuditTrailDTO extends BusinessLogDTO {
    private String flowCode;

    public AuditTrailDTO(String flowCode,
                         String operationCode,
                         String description,
                         String defaultDescription,
                         String value,
                         String defaultValue,
                         String exception,
                         String defaultException,
                         Mode mode) {
        super(operationCode, description, defaultDescription, value, defaultValue, exception, defaultException, mode);
        this.flowCode = flowCode;
    }
}