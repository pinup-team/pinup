package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OAuthTokenRequestException extends GlobalCustomException {
    public OAuthTokenRequestException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
