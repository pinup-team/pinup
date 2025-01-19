package kr.co.pinup.posts.exception.comment;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class CommentPermissionDeniedException extends GlobalCustomException {
    public CommentPermissionDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}