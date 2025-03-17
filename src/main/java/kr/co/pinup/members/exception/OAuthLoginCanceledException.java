package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OAuthLoginCanceledException  extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "로그인을 취소합니다.";

    public OAuthLoginCanceledException() {
        super(DEFAULT_MESSAGE);
    }

    public OAuthLoginCanceledException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
