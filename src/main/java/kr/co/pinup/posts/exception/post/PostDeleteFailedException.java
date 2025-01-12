package kr.co.pinup.posts.exception.post;

public class PostDeleteFailedException extends RuntimeException {
    public PostDeleteFailedException(String message) {
        super(message);
    }
}