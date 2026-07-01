package mx.bastekor.flowweaver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.enums.Phase;

@Setter
@Getter
@NoArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"request_id", "group", "code", "description", "status", "result", "mode", "data"})
public class SimpleRequestDTO extends AppInfoDTO {
    @JsonProperty("request_id")
    private String id;
    @JsonProperty("group")
    private String group;
    @JsonProperty("code")
    private String code;
    @JsonProperty("description")
    private String description;
    @JsonProperty("status")
    private String status;
    @JsonProperty("result")
    private String result;
    @JsonProperty("mode")
    private String mode;
    @JsonProperty("phase")
    private Phase phase;
    @JsonProperty("data")
    private DataDTO data;
}
