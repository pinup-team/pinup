package kr.co.pinup.posts.exception.general;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}