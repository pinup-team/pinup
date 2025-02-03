package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OAuthProviderNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "OAuth 제공자를 찾을 수 없습니다.";

    public OAuthProviderNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public OAuthProviderNotFoundException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
