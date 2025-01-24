package kr.co.pinup.notices.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NoticeNotFound extends GlobalCustomException {

    private static final String MESSAGE = "공지사항이 존재하지 않습니다.";

    public NoticeNotFound() {
        super(MESSAGE);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
