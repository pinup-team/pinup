package kr.co.pinup.exception.common;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoResourceFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "해당 리소스를 찾을 수 없습니다.";

    public NoResourceFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public NoResourceFoundException(String message) {
        super(message);
    }

    public int getHttpStatusCode() {
        return 404;
    }
}
