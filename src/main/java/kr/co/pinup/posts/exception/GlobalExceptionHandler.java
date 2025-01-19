package kr.co.pinup.posts.exception;

import kr.co.pinup.posts.exception.globalcustomapp.GlobalCustomException;
import kr.co.pinup.posts.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GlobalExceptionHandler {

    //TODO 부모 추상화 디벨롭 생각 / dto 분리 / 테스트 코드  강의 시간복작도

    // 공통 에러 응답 빌더 (ErrorResponse DTO 사용)
    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, HttpStatus status, String details) {
        ErrorResponse errorResponse = new ErrorResponse("error", message);
        if (details != null) {
            errorResponse.setDetails(details);
        }
        return ResponseEntity.status(status).body(errorResponse);
    }

    // CustomAppException 핸들러 (모든 하위 클래스 포함)
    @ExceptionHandler(GlobalCustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomAppException(GlobalCustomException ex) {
        return buildErrorResponse(ex.getMessage(), ex.getStatus(), null);
    }

}
