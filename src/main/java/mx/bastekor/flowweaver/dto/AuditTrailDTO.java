package mx.bastekor.flowweaver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
public class AuditTrailDTO extends BusinessLogDTO {
    private String flowCode;
}