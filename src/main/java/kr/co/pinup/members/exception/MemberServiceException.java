package kr.co.pinup.members.exception;

public class MemberServiceException extends RuntimeException {
    public MemberServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
