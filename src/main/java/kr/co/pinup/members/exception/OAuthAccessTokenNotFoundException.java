package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OAuthAccessTokenNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "Access Token을 찾을 수 없습니다.";

    public OAuthAccessTokenNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public OAuthAccessTokenNotFoundException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}