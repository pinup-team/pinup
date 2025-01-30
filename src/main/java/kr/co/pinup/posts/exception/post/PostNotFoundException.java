package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PostNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "게시물을 찾을 수 없습니다.";

    public PostNotFoundException() {
        this(DEFAULT_MESSAGE);
    }

    public PostNotFoundException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
