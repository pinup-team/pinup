package kr.co.pinup.notice.exception;

import org.springframework.http.HttpStatus;

public class NoticeNotFound extends RuntimeException {

    private static final String MESSAGE = "공지사항이 존재하지 않습니다.";

    public NoticeNotFound() {
        super(MESSAGE);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
