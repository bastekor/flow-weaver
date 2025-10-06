package mx.bastekor.flowweaver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"in", "out", "in_out"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataDTO {
    @JsonProperty("in")
    private Map<String, String> dataIn;
    @JsonProperty("out")
    private Map<String, String> dataOut;
    @JsonProperty("in_out")
    private Map<String, String> dataInOut;
}