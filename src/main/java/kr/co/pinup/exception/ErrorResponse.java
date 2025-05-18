package kr.co.pinup.exception;

import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

public record ErrorResponse(int status, String message, Map<String, String> validation) {

    @Builder
    public ErrorResponse(int status, String message, Map<String, String> validation) {
        this.status = status;
        this.message = message;
        this.validation = validation != null ? validation : new HashMap<>();
    }

    public void addValidation(String field, String message) {
        validation.put(field, message);
    }
}