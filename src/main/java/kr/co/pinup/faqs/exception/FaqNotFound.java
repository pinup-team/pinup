package kr.co.pinup.faqs.exception;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FaqNotFound extends GlobalCustomException {

    private static final String MESSAGE = "FAQ가 존재하지 않습니다.";

    public FaqNotFound() {
        super(MESSAGE);
    }

    @Override
    public int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
