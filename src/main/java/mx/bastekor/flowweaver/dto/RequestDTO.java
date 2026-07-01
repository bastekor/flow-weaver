package mx.bastekor.flowweaver.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.resolver.ResolutionResult;

import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestDTO extends SimpleRequestDTO {
    @JsonIgnore
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, ResolutionResult> resolution;
}
