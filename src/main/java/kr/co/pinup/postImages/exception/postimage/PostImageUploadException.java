package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
// check
public class PostImageUploadException extends GlobalCustomException {

    public PostImageUploadException(String message) {
//        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        super(message);
    }

    public PostImageUploadException(String message, Throwable cause) {
//        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause); // 부모 생성자 호출
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}