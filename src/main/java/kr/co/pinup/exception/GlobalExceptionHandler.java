package kr.co.pinup.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidRequestHandler(MethodArgumentNotValidException ex) {
        HttpStatus status = valueOf(ex.getStatusCode().value());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status)
                .message("잘못된 요청입니다.")
                .build();

        for (FieldError fieldError : ex.getFieldErrors()) {
            errorResponse.addValidation(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(status)
                .body(errorResponse);
    }

    @ExceptionHandler(GlobalCustomException.class)
    public ResponseEntity<ErrorResponse> customException(GlobalCustomException ex) {
        HttpStatus status = ex.getHttpStatus();

        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status)
                        .message(ex.getMessage())
                        .validation(ex.getValidation())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exception(Exception ex) {
        log.error("[Exception]", ex);

        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .status(INTERNAL_SERVER_ERROR)
                        .message(ex.getMessage())
                        .build());
    }
}
