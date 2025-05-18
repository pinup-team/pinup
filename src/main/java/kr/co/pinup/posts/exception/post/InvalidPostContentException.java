package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPostContentException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "유효하지 않은 게시물 내용입니다.";

    public InvalidPostContentException() {
        this(DEFAULT_MESSAGE);
    }

    public InvalidPostContentException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
