package mx.bastekor.flowweaver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"request_id", "group", "code", "description", "status", "result", "mode", "data"})
public class RequestDTO extends AppInfoDTO {
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
    private String mode; // Revisar si es necesario saberlo en salida final
    @JsonProperty("data")
    private DataDTO data;
}