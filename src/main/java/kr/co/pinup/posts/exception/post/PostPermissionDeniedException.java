package kr.co.pinup.posts.exception.post;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostPermissionDeniedException extends GlobalCustomException {
    public PostPermissionDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}