package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

// check
public class PostImageNotFoundException extends GlobalCustomException {

    public PostImageNotFoundException(String message) {
//        super(message, HttpStatus.NOT_FOUND);
        super(message);
    }

    public PostImageNotFoundException(String message, Throwable cause) {
//        super(message, HttpStatus.NOT_FOUND, cause); // 부모 생성자 호출
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return 0;
    }
}