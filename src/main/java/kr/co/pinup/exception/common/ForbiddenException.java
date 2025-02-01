package kr.co.pinup.exception.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException  {
    public ForbiddenException(String message) {
        super(message);
    }

    public int getHttpStatusCode() {
        return HttpStatus.FORBIDDEN.value();
    }
}
