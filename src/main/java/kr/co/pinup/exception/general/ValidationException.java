package kr.co.pinup.exception.general;
import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class ValidationException extends GlobalCustomException {
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}