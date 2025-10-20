package mx.bastekor.flowweaver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.enums.Mode;
import org.apache.commons.lang3.StringUtils;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class BusinessLogDTO extends DataParamsDTO {
    private String operationCode;
    private String description;
    private String defaultDescription;
    private String value;
    private String defaultValue;
    private String exception;
    private String defaultException;
    private Mode mode;

    /**
     * Temporal, no necesitamos logica, solo imprimir el contenido por pipes
     * @return
     */
    public String toPipeString() {
        final String pipe = "|";
        return operationCode + pipe +
                StringUtils.defaultIfBlank(this.description, this.defaultDescription) + pipe +
                StringUtils.defaultIfBlank(this.value, this.defaultValue) +  pipe +
                StringUtils.defaultIfBlank(this.exception, this.defaultException) +  pipe +
                mode.toString();
    }
}