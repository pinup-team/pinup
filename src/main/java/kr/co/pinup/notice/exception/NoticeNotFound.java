package kr.co.pinup.notice.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class NoticeNotFound extends GlobalCustomException {

    private static final String MESSAGE = "공지사항이 존재하지 않습니다.";

    public NoticeNotFound() {
        super(MESSAGE);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
