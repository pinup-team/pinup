package kr.co.pinup.storeimages.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StoreThumbnaiImagelNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "해당 스토어 ID에 썸네일 이미지가 존재하지 않습니다.";

    public StoreThumbnaiImagelNotFoundException() {
        this(DEFAULT_MESSAGE);
    }

    public StoreThumbnaiImagelNotFoundException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}

