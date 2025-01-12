package kr.co.pinup.posts.exception.postimage;

public class PostImageDeleteFailedException extends RuntimeException {
    public PostImageDeleteFailedException(String message) {
        super(message);  // 메시지만 전달
    }

    public PostImageDeleteFailedException(String message, Throwable cause) {
        super(message, cause);  // 메시지와 원인 예외를 전달
    }
}
