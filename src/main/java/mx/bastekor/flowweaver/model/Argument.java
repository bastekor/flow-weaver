package mx.bastekor.flowweaver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Argument {
    private int index;
    private String name;
    private String type;
    private Object value;
    private List<String> annotations;
}