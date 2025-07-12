package mx.bastekor.flowweaver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DataParamsDTO {
    private DataParamDTO[] dataIn;
    private DataParamDTO[] dataOut;
    private DataParamDTO[] dataInOut;
}