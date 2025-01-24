package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
// check
public class PostUpdateFailedException extends GlobalCustomException {
    public PostUpdateFailedException(String message) {
//        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}