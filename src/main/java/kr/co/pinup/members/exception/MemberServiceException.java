package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class MemberServiceException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "회원 서비스 요청 중 오류가 발생하였습니다.";

    public MemberServiceException() {
        super(DEFAULT_MESSAGE);
    }

    public MemberServiceException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
