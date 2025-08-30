package kr.co.pinup.verification.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VerificationBadRequestException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "인증 정보가 잘못되었습니다.";

    public VerificationBadRequestException() {
        super(DEFAULT_MESSAGE);
    }

    public VerificationBadRequestException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
