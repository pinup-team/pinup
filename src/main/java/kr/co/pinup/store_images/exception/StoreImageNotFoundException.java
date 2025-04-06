package kr.co.pinup.store_images.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StoreImageNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지 탐색 실패";

    public StoreImageNotFoundException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public StoreImageNotFoundException() {
        this(DEFAULT_MESSAGE);
    }
}

