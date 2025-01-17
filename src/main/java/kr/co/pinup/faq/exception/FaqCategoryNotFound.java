package kr.co.pinup.faq.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class FaqCategoryNotFound extends GlobalCustomException {

    private static final String MESSAGE = "카테고리가 존재하지 않습니다.";

    public FaqCategoryNotFound() {
        this(MESSAGE);
    }

    public FaqCategoryNotFound(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
