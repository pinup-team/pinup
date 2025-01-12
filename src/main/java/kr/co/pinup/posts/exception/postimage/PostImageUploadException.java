package kr.co.pinup.posts.exception.postimage;

public class PostImageUploadException extends RuntimeException {
    public PostImageUploadException(String message) {
        super(message);  // 메시지만 전달
    }

    public PostImageUploadException(String message, Throwable cause) {
        super(message, cause);  // 메시지와 원인 예외를 전달
    }
}
