package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OAuthTokenNotFoundException extends GlobalCustomException {
    public OAuthTokenNotFoundException(String message) {
        super(message);
    }
    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
