package kr.co.pinup.exception;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import kr.co.pinup.api.aws.exception.SecretsManagerFetchException;
import kr.co.pinup.api.kakao.exception.KakaoApiException;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.OAuthLoginCanceledException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AppLogger appLogger;

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

        appLogger.warn(
                new WarnLog("요청 파라미터 유효성 오류")
                        .setStatus(String.valueOf(status))
                        .addDetails("reason", "validation failure","invalidField", ex.getFieldErrors().toString())
        );

        return ResponseEntity.status(status)
                .body(errorResponse);
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MemberBadRequestException.class)
    public ResponseEntity<ErrorResponse> memberBadRequestHandler(MemberBadRequestException ex) {
        int status = BAD_REQUEST.value();

        appLogger.warn(
                new WarnLog("멤버 잘못된 요청: " + ex.getMessage())
                        .setStatus(String.valueOf(status))
                        .addDetails("reason", ex.getMessage())
        );

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
        int status = FORBIDDEN.value();

        appLogger.error(
                new ErrorLog("접근 거부", ex)
                        .setStatus(String.valueOf(status))
        );

        model.addAttribute("error", ErrorResponse.builder()
                .status(status)
                .message("접근 권한이 없습니다.")
                .build());

        return "error";
    }

    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(OAuthLoginCanceledException.class)
    public String loginCanceledHandler(OAuthLoginCanceledException ex) {
        int status = BAD_REQUEST.value();

        appLogger.warn(
                new WarnLog("OAuth 로그인 취소")
                        .setStatus(String.valueOf(status))
        );

        return "index";
    }

    @ResponseBody
    @ExceptionHandler(KakaoApiException.class)
    public ResponseEntity<ErrorResponse> kakaoApiHandler(KakaoApiException ex) {
        final int statusCode = BAD_REQUEST.value();

        appLogger.warn(
                new WarnLog("Kakao API 예외 발생 " + ex)
                        .setStatus(String.valueOf(statusCode))
                        .addDetails("reason", ex.getMessage())
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(statusCode)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest()
                .body(errorResponse);
    }

    @ResponseBody
    @ExceptionHandler(SecretsManagerFetchException.class)
    public ResponseEntity<ErrorResponse> secretsManagerFetchHandler(SecretsManagerFetchException ex) {
        final int statusCode = INTERNAL_SERVER_ERROR.value();

        appLogger.error(
                new ErrorLog("SecretsManagerFetch 예외 발생 ", ex)
                        .setStatus(String.valueOf(statusCode))
                        .addDetails("validation", ex.getMessage())
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(statusCode)
                .message(ex.getMessage())
                .build();

        return ResponseEntity.internalServerError()
                .body(errorResponse);
    }

    @ExceptionHandler(GlobalCustomException.class)
    public String customException(GlobalCustomException ex, HttpServletResponse response, Model model) {
        int status = ex.getHttpStatusCode();

        appLogger.error(
                new ErrorLog("커스텀 예외 발생 ", ex)
                        .setStatus(String.valueOf(status))
                        .addDetails("validation", ex.getValidation().toString())
        );

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
        int status = INTERNAL_SERVER_ERROR.value();

        appLogger.error(
                new ErrorLog("서버 내부 오류", ex)
                        .setStatus(String.valueOf(status))
        );

        model.addAttribute("error", ErrorResponse.builder()
                .status(status)
                .message(ex.getMessage())
                .build());

        response.setStatus(status);

        return "error";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        int status = BAD_REQUEST.value();

        appLogger.warn(
                new WarnLog("제약조건 위반")
                        .setStatus(String.valueOf(status))
                        .addDetails("reason", ex.getMessage())
        );

        return ResponseEntity.status(status)
                .body(new ErrorResponse(status, ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        int status = BAD_REQUEST.value();

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        appLogger.warn(
                new WarnLog("입력값 유효성 오류")
                        .setStatus(String.valueOf(status))
                        .addDetails("reason", "invalid input","invalidFields", errors.toString())
        );

        ErrorResponse errorResponse = new ErrorResponse(
                status,
                "입력값이 유효하지 않습니다.",
                errors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handlerMethodValidationHandler(HandlerMethodValidationException ex) {
        final String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("유효성 검증 실패");

        final int status = BAD_REQUEST.value();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status)
                .message("잘못된 요청입니다.")
                .build();

        errorResponse.addValidation("images", message);

        appLogger.warn(
                new WarnLog(message)
                        .setStatus(String.valueOf(status))
                        .addDetails("reason", "Invalid field images")
        );

        return ResponseEntity.status(status)
                .body(errorResponse);
    }
}