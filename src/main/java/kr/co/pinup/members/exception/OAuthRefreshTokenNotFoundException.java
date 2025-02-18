package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OAuthRefreshTokenNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "Refresh Token을 찾을 수 없습니다.";

    public OAuthRefreshTokenNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public OAuthRefreshTokenNotFoundException(String message) {
        super(message);
    }
    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
