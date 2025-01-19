package kr.co.pinup.posts.exception.general;
import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;


public class InternalServerErrorException extends GlobalCustomException {
    public InternalServerErrorException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}