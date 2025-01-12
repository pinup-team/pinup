package kr.co.pinup.posts.exception.postimage;

public class PostImageSizeLimitExceededException extends RuntimeException {
    public PostImageSizeLimitExceededException(String message) {
        super(message);
    }
}
