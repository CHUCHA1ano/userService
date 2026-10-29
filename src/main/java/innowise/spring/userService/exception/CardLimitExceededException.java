package innowise.spring.userService.exception;

public class CardLimitExceededException extends RuntimeException {

    public CardLimitExceededException(String message) {
        super(message);
    }
}
