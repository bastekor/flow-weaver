package mx.bastekor.flowweaver.factory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mx.bastekor.flowweaver.enums.StatusEnum;
import mx.bastekor.flowweaver.model.Argument;
import mx.bastekor.flowweaver.model.BusinessLogEvent;
import mx.bastekor.flowweaver.model.MethodContext;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BusinessLogEventFactory {
    public static BusinessLogEvent create() {
        return new BusinessLogEvent()
                .setStatus(StatusEnum.SUCCESS)
                .setFlowWeaverContextId("flow-weaver-context-id")
                .setDuration("100 s")
                .setMethodContext(createMethodContext())
                .setBusinessLogDTO(null);
    }

    public static MethodContext createMethodContext() {
        return new MethodContext()
                .setClassName("mx.bastekor.flowweaver.component.TestComponent")
                .setMethodName("doSomething001")
                .setReturnType("String")
                .setArguments(createArguments())
                .setMethodAnnotations(createAnnotations())
                .setOutput("Ok")
                .setException(null);
    }

    private static List<Argument> createArguments() {
        return List.of(new Argument(0, "argA", "String", "Hola", null));
    }

    private static List<String> createAnnotations() {
        return List.of("NotNull", "Size", "Max");
    }
}