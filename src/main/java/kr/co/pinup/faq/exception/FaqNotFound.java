package kr.co.pinup.faq.exception;

import org.springframework.http.HttpStatus;

public class FaqNotFound extends RuntimeException {

    private static final String MESSAGE = "FAQ가 존재하지 않습니다.";

    public FaqNotFound() {
        super(MESSAGE);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
