package kr.co.pinup.posts.exception.postimage;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostImageUploadException extends GlobalCustomException {

    public PostImageUploadException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public PostImageUploadException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause); // 부모 생성자 호출
    }
}