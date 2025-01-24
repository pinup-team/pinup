package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostImageSizeLimitExceededException extends GlobalCustomException {

    public PostImageSizeLimitExceededException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public PostImageSizeLimitExceededException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause); // 부모 생성자 호출
    }
}