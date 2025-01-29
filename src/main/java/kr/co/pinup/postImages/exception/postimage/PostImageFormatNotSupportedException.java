package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;

// check
public class PostImageFormatNotSupportedException extends GlobalCustomException {

    public PostImageFormatNotSupportedException(String message) {
//        super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        super(message);
    }

    public PostImageFormatNotSupportedException(String message, Throwable cause) {
//        super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, cause); // 부모 생성자 호출
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}