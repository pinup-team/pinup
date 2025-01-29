package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
// check
public class PostNotFoundException extends GlobalCustomException {
    public PostNotFoundException(String message) {
//        super(message, HttpStatus.NOT_FOUND);
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}