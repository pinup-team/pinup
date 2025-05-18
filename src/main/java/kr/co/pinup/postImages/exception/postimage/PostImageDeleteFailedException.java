package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PostImageDeleteFailedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지 삭제에 실패했습니다.";

    public PostImageDeleteFailedException() {
        this(DEFAULT_MESSAGE);
    }

    public PostImageDeleteFailedException(String message) {
        super(message);
    }

    public PostImageDeleteFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}

