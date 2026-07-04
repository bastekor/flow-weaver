package mx.bastekor.flowweaver.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class RequestDTO extends SimpleRequestDTO {

    @JsonIgnore
    private Map<String, ResolutionResult> resolutions;
}