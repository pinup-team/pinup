package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostImageNotFoundException extends GlobalCustomException {

    public PostImageNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public PostImageNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, cause); // 부모 생성자 호출
    }
}