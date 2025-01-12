package kr.co.pinup.posts.exception.postimage;

public class PostImageFormatNotSupportedException extends RuntimeException {
    public PostImageFormatNotSupportedException(String message) {
        super(message);
    }
}