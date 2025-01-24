package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostImageDeleteFailedException extends GlobalCustomException {

    public PostImageDeleteFailedException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public PostImageDeleteFailedException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause); // 부모 생성자 호출
    }
}