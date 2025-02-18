package kr.co.pinup.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthRefreshTokenNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.URI;

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
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuth != null && currentAuth.isAuthenticated()) {
                System.out.println("handleAccessTokenNotFound!! Authentication 성공: " + currentAuth.getName());
                // 권한 확인
                System.out.println("권한 정보: " + currentAuth.getAuthorities());
            } else {
                log.error("handleAccessTokenNotFound!! Authentication 실패");
                throw new UnauthorizedException();
            }

            MemberInfo memberInfo = (MemberInfo) currentAuth.getPrincipal();
            if (memberInfo == null) {
                log.error("MemberInfo doesn't exist!");
                throw new OAuth2AuthenticationException();
            }

            String refreshToken = SecurityUtil.getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                log.error("refreshToken is null!");
                throw new OAuthRefreshTokenNotFoundException("Refresh Token이 존재하지 않습니다.");
            }

            // AccessToken 갱신
            OAuthToken token = oAuthService.refresh(memberInfo.getProvider(), refreshToken);
            log.debug("Access Token 갱신 완료 : " + token.getAccessToken());

            // 이제 SecurityContext에 저장하기로 했으니 넣어주기
            SecurityUtil.setAccessTokenInSecurityContext(SecurityUtil.getAccessTokenFromSecurityContext());

            // 새로운 AccessToken을 응답으로 반환
            return ResponseEntity.status(HttpStatus.OK).body(token.getAccessToken());
        } catch (Exception refreshException) {
            log.error("Access Token 갱신 중 오류 발생", ex);
            // refresh 실패 시
            return ResponseEntity.status(BAD_REQUEST).body("Access Token 갱신 실패");
        }
    }

    // TODO OAuthAccessTokenNotFoundException 처리, ResponseEntity 아니고 다시 기존으로 돌아가야함
    @ExceptionHandler(OAuthRefreshTokenNotFoundException.class)
    public ResponseEntity<?> handleRefreshTokenNotFoundException(OAuthAccessTokenNotFoundException ex, HttpServletRequest request) {
        try {
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuth != null && currentAuth.isAuthenticated()) {
                System.out.println("handleAccessTokenNotFound!! Authentication 성공: " + currentAuth.getName());
                // 권한 확인
                System.out.println("권한 정보: " + currentAuth.getAuthorities());
            } else {
                log.error("handleAccessTokenNotFound!! Authentication 실패");
                throw new UnauthorizedException();
            }

            MemberInfo memberInfo = (MemberInfo) currentAuth.getPrincipal();
            if (memberInfo == null) {
                log.error("MemberInfo doesn't exist!");
                throw new OAuth2AuthenticationException();
            }

            String refreshToken = SecurityUtil.getRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                log.error("refreshToken is null!");
                SecurityUtil.clearAccessTokenFromSecurityContext(request);
            }

            // Refresh 없어졌는지 확인
            refreshToken = SecurityUtil.getRefreshTokenFromCookie(request);
            if (refreshToken != null) {
                return ResponseEntity.status(BAD_REQUEST).body("강제 로그아웃에 실패하였습니다. : " + ex);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/"));

            // TODO 강제 로그아웃 성공함, 이 다음에 어떻게 ALERT 띄우고 메인으로 보낼지 생각해보기
            return ResponseEntity.ok("강제 로그아웃 되었습니다.");
        } catch (Exception refreshException) {
            log.error("강제 로그아웃 중 오류 발생", ex);
            // refresh 실패 시
            return ResponseEntity.status(BAD_REQUEST).body("강제 로그아웃 실패");
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