package dk.xam.jgccli.exception;

public class AuthorizationException extends GccliException {
    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
