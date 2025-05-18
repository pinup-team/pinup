package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PostPermissionDeniedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "게시물에 대한 접근 권한이 없습니다.";

    public PostPermissionDeniedException() {
        this(DEFAULT_MESSAGE);
    }

    public PostPermissionDeniedException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.FORBIDDEN.value();
    }
}
