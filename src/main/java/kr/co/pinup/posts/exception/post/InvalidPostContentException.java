package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
// check
public class InvalidPostContentException extends GlobalCustomException {
    public InvalidPostContentException(String message) {
//        super(message, HttpStatus.BAD_REQUEST);
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}
