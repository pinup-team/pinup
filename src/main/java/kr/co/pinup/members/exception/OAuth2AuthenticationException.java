package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class OAuth2AuthenticationException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "OAuth 로그인 중 오류가 발생했습니다.";

    public OAuth2AuthenticationException() {
        super(DEFAULT_MESSAGE);
    }

    public OAuth2AuthenticationException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.UNAUTHORIZED.value();
    }
}