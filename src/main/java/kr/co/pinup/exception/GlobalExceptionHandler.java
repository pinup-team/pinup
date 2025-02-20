package kr.co.pinup.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // TODO OAuthAccessTokenNotFoundException 처리, ResponseEntity 아니고 다시 기존으로 돌아가야함
    @ExceptionHandler(OAuthAccessTokenNotFoundException.class)
    public ResponseEntity<?> handleAccessTokenNotFound(OAuthAccessTokenNotFoundException ex, HttpServletRequest request) {
        try {
            MemberInfo memberInfo = SecurityUtil.getMemberInfo();

            String refreshToken = SecurityUtil.getOptionalRefreshToken(request);
            if (refreshToken == null) {
                log.error("handleAccessTokenNotFound!! refreshToken is null");
                throw new OAuth2AuthenticationException("로그인 정보가 없습니다.");
            }

            // AccessToken 갱신
            OAuthToken token = oAuthService.refresh(memberInfo.getProvider(), refreshToken);
            log.debug("Access Token 갱신 완료 : " + token.getAccessToken());

            // 이제 SecurityContext에 저장하기로 했으니 넣어주기
            SecurityUtil.refreshAccessTokenInSecurityContext(token.getAccessToken());

            // 새로운 AccessToken을 응답으로 반환
            return ResponseEntity.status(HttpStatus.OK).body(token.getAccessToken());
        } catch (Exception e) {
            log.error("Access Token 갱신 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 만료되었습니다.");
        }
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