package kr.co.pinup.posts.exception.postimage;

public class PostImageNotFoundException extends RuntimeException {
    public PostImageNotFoundException(String message) {
        super(message);
    }
}
