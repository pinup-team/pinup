package kr.co.pinup.posts.exception;

import kr.co.pinup.posts.exception.comment.CommentNotFoundException;
import kr.co.pinup.posts.exception.general.BadRequestException;
import kr.co.pinup.posts.exception.general.InternalServerErrorException;
import kr.co.pinup.posts.exception.general.ValidationException;
import kr.co.pinup.posts.exception.post.InvalidPostContentException;
import kr.co.pinup.posts.exception.post.PostPermissionDeniedException;
import kr.co.pinup.posts.exception.postimage.*;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 404 예외 처리: PostNotFoundException (게시글을 찾을 수 없음)
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePostNotFoundException(PostNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 400 예외 처리: InvalidPostContentException (잘못된 게시글 내용)
    @ExceptionHandler(InvalidPostContentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPostContentException(InvalidPostContentException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 500 예외 처리: PostImageUploadException (이미지 업로드 실패)
    @ExceptionHandler(PostImageUploadException.class)
    public ResponseEntity<Map<String, String>> handlePostImageUploadException(PostImageUploadException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // 404 예외 처리: CommentNotFoundException (댓글을 찾을 수 없음)
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCommentNotFoundException(CommentNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // 403 예외 처리: PostPermissionDeniedException (게시글 권한 없음)
    @ExceptionHandler(PostPermissionDeniedException.class)
    public ResponseEntity<Map<String, String>> handlePostPermissionDeniedException(PostPermissionDeniedException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // 500 예외 처리: PostImageNotFoundException (이미지를 찾을 수 없음)
    @ExceptionHandler(PostImageNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePostImageNotFoundException(PostImageNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // 500 예외 처리: PostImageDeleteFailedException (이미지 삭제 실패)
    @ExceptionHandler(PostImageDeleteFailedException.class)
    public ResponseEntity<Map<String, String>> handlePostImageDeleteFailedException(PostImageDeleteFailedException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // 400 예외 처리: PostImageSizeLimitExceededException (이미지 크기 초과)
    @ExceptionHandler(PostImageSizeLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handlePostImageSizeLimitExceededException(PostImageSizeLimitExceededException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 415 예외 처리: PostImageFormatNotSupportedException (이미지 형식 미지원)
    @ExceptionHandler(PostImageFormatNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handlePostImageFormatNotSupportedException(PostImageFormatNotSupportedException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    // 500 예외 처리: InternalServerErrorException (서버 내부 오류)
    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<Map<String, String>> handleInternalServerErrorException(InternalServerErrorException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // 400 예외 처리: BadRequestException (잘못된 요청)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequestException(BadRequestException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 400 예외 처리: ValidationException (유효성 검증 오류)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // 전역 예외 처리 (기타 예외 처리)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
