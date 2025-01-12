package kr.co.pinup.posts.exception.post;

public class PostPermissionDeniedException extends RuntimeException {
    public PostPermissionDeniedException(String message) {
        super(message);
    }
}