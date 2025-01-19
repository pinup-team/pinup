package kr.co.pinup.posts.exception.comment;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class CommentNotFoundException extends GlobalCustomException {
    public CommentNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
