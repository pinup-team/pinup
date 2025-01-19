package kr.co.pinup.posts.exception.post;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostUpdateFailedException extends GlobalCustomException {
    public PostUpdateFailedException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}