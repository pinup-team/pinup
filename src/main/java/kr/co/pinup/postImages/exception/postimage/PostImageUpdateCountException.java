package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PostImageUpdateCountException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지는 최소 2장 이상이어야 합니다.";

    public PostImageUpdateCountException() {
        this(DEFAULT_MESSAGE);
    }

    public PostImageUpdateCountException(String message) {
        super(message);
    }

    public PostImageUpdateCountException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}

