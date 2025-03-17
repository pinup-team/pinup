package kr.co.pinup.exception;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.OAuthLoginCanceledException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> invalidRequestHandler(BindException ex) {
        int status = BAD_REQUEST.value();
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

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MemberBadRequestException.class)
    public ResponseEntity<ErrorResponse> memberBadRequestHandler(MemberBadRequestException ex) {
        int status = BAD_REQUEST.value();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(status)
                .body(errorResponse);
    }

    @ResponseStatus(FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public String accessDeniedHandler(AccessDeniedException ex, Model model) {
        model.addAttribute("error", ErrorResponse.builder()
                .status(FORBIDDEN.value())
                .message("접근 권한이 없습니다.")
                .build());

        return "error";
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(OAuthLoginCanceledException.class)
    public String loginCanceledHandler(OAuthLoginCanceledException ex) {
        log.debug("login canceled");
        return "index";
    }

    @ExceptionHandler(GlobalCustomException.class)
    public String customException(GlobalCustomException ex, HttpServletResponse response, Model model) {
        int status = ex.getHttpStatusCode();
        model.addAttribute("error", ErrorResponse.builder()
                .status(status)
                .message(ex.getMessage())
                .validation(ex.getValidation())
                .build());

        response.setStatus(status);

        return "error";
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String exception(Exception ex, HttpServletResponse response, Model model) {
        log.error("[Exception]", ex);
        int status = INTERNAL_SERVER_ERROR.value();
        model.addAttribute("error", ErrorResponse.builder()
                .status(status)
                .message(ex.getMessage())
                .build());

        response.setStatus(status);

        return "error";
    }
}