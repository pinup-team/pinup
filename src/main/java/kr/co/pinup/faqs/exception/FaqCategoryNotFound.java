package kr.co.pinup.faqs.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FaqCategoryNotFound extends GlobalCustomException {

    private static final String MESSAGE = "카테고리가 존재하지 않습니다.";

    public FaqCategoryNotFound() {
        this(MESSAGE);
    }

    public FaqCategoryNotFound(String message) {
        super(message);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
