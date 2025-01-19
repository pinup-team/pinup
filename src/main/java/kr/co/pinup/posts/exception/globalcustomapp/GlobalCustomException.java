package kr.co.pinup.posts.exception.globalcustomapp;

import org.springframework.http.HttpStatus;

public class GlobalCustomException extends RuntimeException {

    private final HttpStatus status;

    public GlobalCustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public GlobalCustomException(String message, HttpStatus status, Throwable cause) {
        super(message, cause); // 부모 RuntimeException에 메시지와 원인 전달
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
