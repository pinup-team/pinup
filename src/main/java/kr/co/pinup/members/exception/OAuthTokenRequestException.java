package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OAuthTokenRequestException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "OAuth 토큰 요청이 잘못되었습니다.";

    public OAuthTokenRequestException() {
        super(DEFAULT_MESSAGE);
    }

    public OAuthTokenRequestException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}
