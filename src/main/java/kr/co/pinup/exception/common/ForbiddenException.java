package kr.co.pinup.exception.common;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "접근 권한이 없습니다.";

    public ForbiddenException() {
        super(DEFAULT_MESSAGE);
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public int getHttpStatusCode() {
        return HttpStatus.FORBIDDEN.value();
    }
}
