package kr.co.pinup.exception.general;

import kr.co.pinup.exception.GlobalCustomException;

// check
public class ValidationException extends GlobalCustomException {
    public ValidationException(String message) {
//        super(message, HttpStatus.BAD_REQUEST);
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}