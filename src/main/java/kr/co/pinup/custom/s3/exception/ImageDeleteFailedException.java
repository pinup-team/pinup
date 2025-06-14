package kr.co.pinup.custom.s3.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ImageDeleteFailedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지 삭제에 실패했습니다.";

    public ImageDeleteFailedException() {
        this(DEFAULT_MESSAGE);
    }

    public ImageDeleteFailedException(String message) {
        super(message);
    }

    public ImageDeleteFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
