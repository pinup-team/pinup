package kr.co.pinup.posts.exception.general;
import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class ValidationException extends GlobalCustomException {
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}