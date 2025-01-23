package kr.co.pinup.exception;

import org.springframework.http.HttpStatus;

public class GlobalCustomException extends RuntimeException {

    private final HttpStatus status;

    public GlobalCustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public GlobalCustomException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
