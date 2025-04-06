package kr.co.pinup.posts.exception.post;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ImageCountException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지는 최소 2장 이상 등록해야 합니다.";

    public ImageCountException() {
        this(DEFAULT_MESSAGE);
    }

    public ImageCountException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
