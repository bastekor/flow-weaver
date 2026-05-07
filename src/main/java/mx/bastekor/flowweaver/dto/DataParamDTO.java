package mx.bastekor.flowweaver.dto;

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
public class DataParamDTO {
    private String key; // Campo a modificar/asignar en persistencia.
    private String value; // Valor a buscar mediante SPeL o custom.
    private String defaultValue; // Valor por defecto estático, asignado en caso de no encontrar valor.
}