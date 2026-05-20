package mx.bastekor.flowweaver.resolver;

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
public class ResolutionError {
    private String message;
    private String lastPath;
    private List<String> suggestions;
}
