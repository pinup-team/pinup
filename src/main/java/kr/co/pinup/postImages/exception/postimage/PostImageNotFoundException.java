package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PostImageNotFoundException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지를 찾을 수 없습니다.";

    public PostImageNotFoundException() {
        this(DEFAULT_MESSAGE);
    }

    public PostImageNotFoundException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.NOT_FOUND.value();
    }
}
