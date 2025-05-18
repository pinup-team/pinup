package kr.co.pinup.exception.common;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "인증 정보가 없습니다.";

    public UnauthorizedException() {
        super(DEFAULT_MESSAGE);
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public int getHttpStatusCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}