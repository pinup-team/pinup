package kr.co.pinup.posts.exception.postimage;

import kr.co.pinup.posts.exception.customapp.CustomAppException;
import org.springframework.http.HttpStatus;

public class PostImageAmazonServiceException extends CustomAppException {

    public PostImageAmazonServiceException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public PostImageAmazonServiceException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}

