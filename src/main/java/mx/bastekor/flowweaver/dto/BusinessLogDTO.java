package mx.bastekor.flowweaver.dto;

import lombok.AllArgsConstructor;
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
public class BusinessLogDTO extends DataParamsDTO {
    private String flowCode;
    private String description;
    private String defaultDescription;
    private String value;
    private String defaultValue;
    private String exception;
    private String defaultException;
    private Mode mode;
}