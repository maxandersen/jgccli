package dk.xam.jgccli.exception;

public class GccliException extends RuntimeException {
    public GccliException(String message) {
        super(message);
    }

    public GccliException(String message, Throwable cause) {
        super(message, cause);
    }
}
