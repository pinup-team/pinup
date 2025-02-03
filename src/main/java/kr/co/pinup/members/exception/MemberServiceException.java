package kr.co.pinup.members.exception;

import kr.co.pinup.exception.GlobalCustomException;

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
        return 0;
    }
}
