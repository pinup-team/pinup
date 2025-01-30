package kr.co.pinup.postImages.exception.postimage;

import kr.co.pinup.exception.GlobalCustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PostImageAmazonServiceException extends GlobalCustomException {

    private static final String DEFAULT_MESSAGE = "Amazon S3 서비스에서 문제가 발생했습니다.";

    public PostImageAmazonServiceException() {
        this(DEFAULT_MESSAGE);
    }

    public PostImageAmazonServiceException(String message) {
        super(message);
    }

    @Override
    protected int getHttpStatusCode() {
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
