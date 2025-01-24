package kr.co.pinup.comments.exception.comment;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

// check
public class CommentNotFoundException extends GlobalCustomException {
    public CommentNotFoundException(String message) {
//        super(message, HttpStatus.NOT_FOUND);
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}
