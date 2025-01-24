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
import org.springframework.web.servlet.ModelAndView;

import java.util.Iterator;
import java.util.List;

// TODO valid 검증 400, 404일 경우 메시지 담아서 전달해서 뿌려주기
// CHECK 500, 401 에러일 경우에는 ERROR, LOGIN 페이지로 이동
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("BadRequest : ", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(Exception e) {
        log.error("리소스 없음 : ", e.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "리소스를 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ModelAndView handleUnauthorizedException(UnauthorizedException e) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("message", "로그인이 필요합니다.");
        return modelAndView;
    }

    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView handleForbiddenException(Exception e) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("index");
        modelAndView.addObject("message", "권한이 없습니다.");
        return modelAndView;
    }

//    @ExceptionHandler(UnauthorizedException.class)
//    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
//        log.error("로그인 만료 : ", e.getMessage());
//        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "로그인이 필요합니다.");
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//    }
//
//    @ExceptionHandler(ForbiddenException.class)
//    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException e) {
//        log.error("권한 없음 : ", e.getMessage());
//        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "권한이 없습니다.");
//        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
//        log.error("서버 예외 발생 : ", e.getMessage());
//        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(errorResponse);
//    }

    // https://kdhyo98.tistory.com/81#google_vignette
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<ObjectError> allErrors = e.getAllErrors();
        String message = getMessage(allErrors.iterator());

        ErrorResponse result = ErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(message).build();

        return ResponseEntity.badRequest().body(result);
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
}
