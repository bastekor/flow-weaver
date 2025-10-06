package mx.bastekor.flowweaver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.enums.StatusEnum;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class BusinessLogEvent {
    private String flowWeaverContextId;
    private String duration;
    private StatusEnum status;
    private MethodContext methodContext;
    private BusinessLogDTO businessLogDTO;
}