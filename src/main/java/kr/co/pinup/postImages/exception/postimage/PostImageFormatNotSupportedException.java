package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class PostImageFormatNotSupportedException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "지원되지 않는 이미지 형식입니다.";

    public PostImageFormatNotSupportedException() {
        this(DEFAULT_MESSAGE);
    }

    public PostImageFormatNotSupportedException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.UNSUPPORTED_MEDIA_TYPE.value();
    }
}
