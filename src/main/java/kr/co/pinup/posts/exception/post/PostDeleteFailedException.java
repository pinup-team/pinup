package kr.co.pinup.posts.exception.post;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostDeleteFailedException extends GlobalCustomException {
    public PostDeleteFailedException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}