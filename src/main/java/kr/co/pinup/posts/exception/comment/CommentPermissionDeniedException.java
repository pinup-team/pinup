package kr.co.pinup.posts.exception.comment;

public class CommentPermissionDeniedException extends RuntimeException {
    public CommentPermissionDeniedException(String message) {
        super(message);
    }
}