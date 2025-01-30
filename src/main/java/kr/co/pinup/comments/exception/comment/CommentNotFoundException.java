package kr.co.pinup.comments.exception.comment;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CommentNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "댓글을 찾을 수 없습니다.";

    public CommentNotFoundException() {
        this(DEFAULT_MESSAGE);
    }

    public CommentNotFoundException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}