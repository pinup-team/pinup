package kr.co.pinup.store_images.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class StoreImageDeleteFailedException extends GlobalCustomException {
    private static final String DEFAULT_MESSAGE = "이미지 삭제 실패";

    public StoreImageDeleteFailedException() {
        this(DEFAULT_MESSAGE);
    }

    public StoreImageDeleteFailedException(String message) {
        super(message);
    }

    public StoreImageDeleteFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}



