package mx.bastekor.flowweaver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MethodContext {
    private String className;
    private String methodName;
    private String returnType;
    private List<String> methodAnnotations;
    private List<MethodArg> arguments;
    private Object output;
    private Throwable exception;
}