package kr.co.pinup.posts.exception.post;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class InvalidPostContentException extends GlobalCustomException {
    public InvalidPostContentException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
