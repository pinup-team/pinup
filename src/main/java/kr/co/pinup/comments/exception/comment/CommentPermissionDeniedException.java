package kr.co.pinup.comments.exception.comment;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class CommentPermissionDeniedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "댓글 권한이 없습니다.";

    public CommentPermissionDeniedException() {
        this(DEFAULT_MESSAGE);
    }

    public CommentPermissionDeniedException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.FORBIDDEN.value();
    }
}
