package mx.bastekor.flowweaver.resolver;

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
public class ResolutionResult {
    private String snapshot;
    private String rootScope;
    private String expression;
    private String suggested;
    private String value;
    private long durationMs;
    private String resolvedPath;
    private ResolutionError error;
}
