package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MemberNotFoundException extends GlobalCustomException {

    private static final String MESSAGE = "사용자를 찾을 수 없습니다.";

    public MemberNotFoundException() {
        super(MESSAGE);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
