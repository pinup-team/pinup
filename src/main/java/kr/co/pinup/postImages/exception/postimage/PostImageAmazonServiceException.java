package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;

// check
public class PostImageAmazonServiceException extends GlobalCustomException {

    public PostImageAmazonServiceException(String message) {
//        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        super(message);
    }

    public PostImageAmazonServiceException(String message, Throwable cause) {
//        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}

