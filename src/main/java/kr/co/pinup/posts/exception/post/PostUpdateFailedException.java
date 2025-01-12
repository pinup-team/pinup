package kr.co.pinup.posts.exception.post;

public class PostUpdateFailedException extends RuntimeException {
    public PostUpdateFailedException(String message) {
        super(message);
    }
}