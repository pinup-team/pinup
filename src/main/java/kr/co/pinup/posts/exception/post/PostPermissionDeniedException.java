package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
// check
public class PostPermissionDeniedException extends GlobalCustomException {
    public PostPermissionDeniedException(String message) {
//        super(message, HttpStatus.FORBIDDEN);
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}