package kr.co.pinup.faq.exception;

import org.springframework.http.HttpStatus;

public class FaqCategoryNotFound extends RuntimeException {

    public FaqCategoryNotFound(String message) {
        super(message);
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
