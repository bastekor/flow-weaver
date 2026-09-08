package mx.bastekor.flowweaver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mx.bastekor.flowweaver.dto.BusinessLogDTO;
import mx.bastekor.flowweaver.dto.MethodSnapshotDTO;
import mx.bastekor.flowweaver.dto.RequestDTO;

import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FlowWeaverResponse {
    private String methodSignature;
    private String methodResponse;
    private String mappedDuration;
    private MethodSnapshotDTO<?> methodSnapshotDTO;
    private BusinessLogDTO annotationDTO;
    private RequestDTO requestDTO;
    /*
    En primer instancia hace el aplanado del objeto RequestDTO, pero este
    soporta aún más, como en el caso del methodDuration y mappedDuration.
    En "teoría" este es el objeto que deberán de usar los implementadores
    para facilitarles el uso, pero se complementa con los demás objetos.
     */
    private Map<String, Object> fields;
}