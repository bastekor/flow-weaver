package mx.bastekor.flowweaver.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MethodDTO {

    @JsonProperty("_class")
    private String className;
    @JsonProperty("_method")
    private String methodName;
    @JsonProperty("_returnType")
    private String returnType;
    @JsonProperty("_annotations")
    private List<String> annotations;
    @JsonProperty("_args")
    private List<ArgumentDTO> arguments;
    @JsonProperty("args")
    private List<String> args;
}