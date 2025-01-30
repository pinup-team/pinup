package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PostDeleteFailedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "게시물 삭제에 실패했습니다.";

    public PostDeleteFailedException() {
        this(DEFAULT_MESSAGE);
    }

    public PostDeleteFailedException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
