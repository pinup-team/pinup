package kr.co.pinup.faq.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class FaqNotFound extends GlobalCustomException {

    private static final String MESSAGE = "FAQ가 존재하지 않습니다.";

    public FaqNotFound() {
        super(MESSAGE);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
