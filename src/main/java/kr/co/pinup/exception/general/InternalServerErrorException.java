package kr.co.pinup.exception.general;
import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;


public class InternalServerErrorException extends GlobalCustomException {
    public InternalServerErrorException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}