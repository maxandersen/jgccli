package dk.xam.jgccli.exception;

public class AccountNotFoundException extends GccliException {
    public AccountNotFoundException(String email) {
        super("Account '" + email + "' not found");
    }
}
