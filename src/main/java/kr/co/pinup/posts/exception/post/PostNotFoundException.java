package kr.co.pinup.posts.exception.post;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import org.springframework.http.HttpStatus;

public class PostNotFoundException extends GlobalCustomException {
    public PostNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}