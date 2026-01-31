package mx.bastekor.flowweaver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
public class DataParamsDTO {
    private DataParamDTO[] dataIn;
    private DataParamDTO[] dataOut;
    private DataParamDTO[] dataInOut;
}