package kr.co.pinup.custom.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthRefreshTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// TODO 체크하기
public class SecurityUtil {

    // 헤더에 AccessToken을 추가하는 메서드
    public static void addAccessTokenToHeader(HttpHeaders headers, String accessToken) {
        headers.add("Authorization", "Bearer " + accessToken);
    }

    public static String getAccessTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            System.out.println("AccessToken from header: " + accessToken);
            return accessToken;
        }
        throw new OAuthTokenNotFoundException("Access token is missing in header.");
    }

    public static void setAccessTokenInSecurityContext(String accessToken) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();

        if (currentAuth != null && currentAuth.isAuthenticated()) {
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    currentAuth.getPrincipal(),
                    currentAuth.getCredentials(),
                    currentAuth.getAuthorities()
            );

            ((UsernamePasswordAuthenticationToken) newAuth).setDetails(accessToken);
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        } else {
            throw new UnauthorizedException("SecurityUtil setAccessTokenInSecurityContext : 인증 정보가 없습니다.");
        }
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();  // 현재 인증된 사용자의 이름 반환
        } else throw new UnauthorizedException("SecurityUtil getCurrentUsername : 인증 정보가 없습니다.");
    }

    public static String getAccessTokenFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) { // SecurityContext가 비어있지 않고, 인증된 사용자일 경우
            return (String) authentication.getDetails(); // accessToken이 details에 저장되었을 경우 반환
        }
        return null;
    }

    // RefreshToken을 반환하는 메서드
    public static String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new OAuthRefreshTokenNotFoundException("Refresh token is missing.");
    }

    public static void setRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
//        cookie.setMaxAge(60 * 60 * 12);
        cookie.setMaxAge(60 * 2);
        cookie.setSecure(false);
        response.addCookie(cookie);
        System.out.println("SecurityUtil : Refresh Token 쿠키에 저장됨: " + cookie.getValue());
    }

    public static void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 쿠키 만료
        cookie.setSecure(false);
        response.addCookie(cookie);
        System.out.println("Refresh Token 쿠키 삭제 완료");
    }

    // TODO AccessToken과 RefreshToken을 AccessToken만 없을 경우 처리하는 메서드
    public static void clearContextWithRefreshToken(HttpServletRequest httpServletRequest) {
        System.out.println("SecurityUtil clearContextWithRefreshToken");
        String accessToken = getAccessTokenFromSecurityContext();
        if (accessToken == null) {
            String refreshToken = getRefreshTokenFromCookie(httpServletRequest);
            if (refreshToken != null) {
                SecurityContextHolder.clearContext();
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    HttpServletResponse response = attributes.getResponse();

                    if (response != null) {
                        clearRefreshTokenCookie(response);
                    }

                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                }
            }
        } else {
            throw new OAuth2AuthenticationException("Access Token이 존재합니다.");
        }
    }

    // TODO AccessToken과 RefreshToken을 체크하여 없을 경우 처리하는 메서드
    // 아마 ACCESSTOKEN없으면 REFRESHTOKEN 호출하니까 그때 REFRESHNOTFOUNDEXCEPTION 써서 호출하면 될듯?
    public static void clearAccessTokenFromSecurityContext(HttpServletRequest httpServletRequest) {
        System.out.println("SecurityUtil clearAccessTokenFromSecurityContext");
        String refreshToken = getRefreshTokenFromCookie(httpServletRequest);

        if (refreshToken == null) {
            String accessToken = getAccessTokenFromSecurityContext();
            System.out.println("accessToken = " + accessToken);
            // SecurityContext 비우기
            SecurityContextHolder.clearContext();

            // 세션 종료
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                HttpServletResponse response = attributes.getResponse();

                if (response != null) {
                    // refresh_token 쿠키 삭제
                    clearRefreshTokenCookie(response);
                }

                // 세션 무효화
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();  // 세션 무효화
                }
            }
        }
    }
}