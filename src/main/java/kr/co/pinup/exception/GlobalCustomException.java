package kr.co.pinup.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class GlobalCustomException extends RuntimeException {

    private final Map<String, String> validation = new HashMap<>();

    public GlobalCustomException(String message) {
        super(message);
    }

    public GlobalCustomException(String message, Throwable cause) {
        super(message, cause);
    }

    protected abstract int getHttpStatusCode();

    public void addValidation(String fieldName, String message) {
        validation.put(fieldName, message);
    }
}
