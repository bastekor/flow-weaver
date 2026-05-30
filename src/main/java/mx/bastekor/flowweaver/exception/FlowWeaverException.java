package mx.bastekor.flowweaver.exception;

import lombok.Getter;
import mx.bastekor.flowweaver.enums.StatusEnum;

@Getter
public class FlowWeaverException extends RuntimeException {

    private final StatusEnum status;

    public FlowWeaverException(String message, StatusEnum status) {
        super(message);
        this.status = status;
    }

    public FlowWeaverException(String message, StatusEnum status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}