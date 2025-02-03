package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MemberBadRequestException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "회원 정보가 잘못되었습니다.";

    public MemberBadRequestException() {
        super(DEFAULT_MESSAGE);
    }

    public MemberBadRequestException(String message) {
        super(message);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}