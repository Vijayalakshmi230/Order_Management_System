package exceptions;

public class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) {
        super("UserNotFoundException"+message);
    }
}
