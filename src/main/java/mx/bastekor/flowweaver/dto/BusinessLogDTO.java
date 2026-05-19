package mx.bastekor.flowweaver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.enums.Mode;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
public class BusinessLogDTO extends DataParamsDTO {
    private String correlationId;
    private String group;
    private String code;
    private String description;
    private String defaultDescription;
    private String value;
    private String defaultValue;
    private String exception;
    private String defaultException;
    private Mode mode;
}