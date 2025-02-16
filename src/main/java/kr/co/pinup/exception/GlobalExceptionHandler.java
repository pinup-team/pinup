package kr.co.pinup.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFound;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.OAuthTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final OAuthService oAuthService;

    public GlobalExceptionHandler(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    @ResponseBody
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidRequestHandler(MethodArgumentNotValidException ex) {
        int status = ex.getStatusCode().value();
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

    // TODO OAuthAccessTokenNotFound 처리
    @ExceptionHandler(OAuthAccessTokenNotFound.class)
    public ResponseEntity<?> handleAccessTokenNotFound(OAuthAccessTokenNotFound ex, HttpServletRequest request) {
        try {
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuth != null && currentAuth.isAuthenticated()) {
                System.out.println("Authentication 성공: " + currentAuth.getName());
                // 권한 확인
                System.out.println("권한 정보: " + currentAuth.getAuthorities());
            } else {
                System.out.println("Authentication 실패");
            }

            MemberInfo memberInfo = (MemberInfo) currentAuth.getPrincipal();
            if(memberInfo == null) {
                throw new OAuth2AuthenticationException();
            }

            String refreshToken = OAuthTokenUtils.getRefreshTokenFromCookie(request);

            // AccessToken 갱신
            OAuthToken token = oAuthService.refresh(memberInfo.getProvider(), refreshToken);

            // 새로운 AccessToken을 응답으로 반환
            return ResponseEntity.status(HttpStatus.OK).body("Access Token을 갱신했습니다.");
        } catch (Exception refreshException) {
            // refresh 실패 시
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Access Token 갱신 실패");
        }
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