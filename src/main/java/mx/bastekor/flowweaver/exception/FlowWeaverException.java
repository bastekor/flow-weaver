package mx.bastekor.flowweaver.exception;

public class FlowWeaverException extends RuntimeException {
    public FlowWeaverException(String message) {
        super(message);
    }

    public FlowWeaverException(String message, Throwable cause) {
        super(message, cause);
    }
}