package kr.co.pinup.posts.exception.postimage;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostImageNotFoundException extends GlobalCustomException {

    public PostImageNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public PostImageNotFoundException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, cause); // 부모 생성자 호출
    }
}