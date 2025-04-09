package exceptions;

public class OrderNotFoundException extends Exception {
    public OrderNotFoundException(String message) {
        super("OrderNotFOundException"+message);
    }
}
