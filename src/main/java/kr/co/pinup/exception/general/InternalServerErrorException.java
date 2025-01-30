package kr.co.pinup.exception.general;

import kr.co.pinup.exception.GlobalCustomException;

// check
public class InternalServerErrorException extends GlobalCustomException {
    public InternalServerErrorException(String message) {
//        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}