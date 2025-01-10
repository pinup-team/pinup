package kr.co.pinup.faq.exception;

import kr.co.pinup.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class FaqExceptionHandler {

    @ExceptionHandler(FaqCategoryNotFound.class)
    public ResponseEntity<ErrorResponse> faqCategoryNotFoundHandler(FaqCategoryNotFound e) {
        HttpStatus status = e.getHttpStatus();

        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status)
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(FaqNotFound.class)
    public ResponseEntity<ErrorResponse> faqNotFoundHandler(FaqNotFound e) {
        HttpStatus status = e.getHttpStatus();

        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status)
                        .message(e.getMessage())
                        .build());
    }
}
