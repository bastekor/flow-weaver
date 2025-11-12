package mx.bastekor.flowweaver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ArgumentDTO {

    @JsonProperty("arg")
    private Integer index;
    private String name;

    private String type;
    private String clazz;
    private String string;

    private Object value;
    private String snapshot;
    private List<String> annotations;
}