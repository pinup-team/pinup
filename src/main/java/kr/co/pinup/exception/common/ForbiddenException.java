package kr.co.pinup.exception.common;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends GlobalCustomException {
    public ForbiddenException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.FORBIDDEN.value();
    }
}
