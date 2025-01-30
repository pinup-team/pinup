package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PostImageSizeLimitExceededException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "이미지 크기 제한을 초과했습니다.";

    public PostImageSizeLimitExceededException() {
        this(DEFAULT_MESSAGE);
    }

    public PostImageSizeLimitExceededException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }
}
