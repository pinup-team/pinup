package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class InvalidPostContentException extends GlobalCustomException {
    public InvalidPostContentException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
