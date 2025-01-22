package kr.co.pinup.exception;

import kr.co.pinup.exception.common.ForbiddenException;
import kr.co.pinup.exception.common.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.resource.NoResourceFoundException;

import java.util.Iterator;
import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getAllErrors();
        String message = getMessage(allErrors.iterator());

        ErrorResponse result = ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message).build();

        return ResponseEntity.badRequest().body(result.toString());
    }

    private String getMessage(Iterator<ObjectError> errorIterator) {
        final StringBuilder resultMessageBuilder = new StringBuilder();
        while (errorIterator.hasNext()) {
            ObjectError error = errorIterator.next();
            resultMessageBuilder
                    .append("['")
                    .append(((FieldError) error).getField()) // 유효성 검사가 실패한 속성
                    .append("' is '")
                    .append(((FieldError) error).getRejectedValue()) // 유효하지 않은 값
                    .append("' :: ")
                    .append(error.getDefaultMessage()) // 유효성 검사 실패 시 메시지
                    .append("]");

            if (errorIterator.hasNext()) {
                resultMessageBuilder.append(", ");
            }
        }

        log.error(resultMessageBuilder.toString());
        return resultMessageBuilder.toString();
    }


    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("리소스를 찾을 수 없습니다.");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한이 없습니다.");
    }

//    @ExceptionHandler(Exception.class)
//    public ModelAndView handleGenericException(Exception e) {
//        ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("error");
//        modelAndView.addObject("message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
//        return modelAndView;
//    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        log.error("서버에서 예외가 발생했습니다: ", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
