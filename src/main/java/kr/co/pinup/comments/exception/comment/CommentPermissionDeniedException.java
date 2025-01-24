package kr.co.pinup.comments.exception.comment;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class CommentPermissionDeniedException extends GlobalCustomException {
    public CommentPermissionDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}