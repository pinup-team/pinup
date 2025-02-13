package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PostImageSaveFailedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지 저장에 실패했습니다.";

    public PostImageSaveFailedException() {
        this(DEFAULT_MESSAGE);
    }

    public PostImageSaveFailedException(String message) {
        super(message);
    }

    public PostImageSaveFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
