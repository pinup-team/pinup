package kr.co.pinup.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFound;
import kr.co.pinup.members.exception.OAuthTokenNotFoundException;
import org.springframework.http.HttpHeaders;

public class OAuthTokenUtils {

    public static String getAccessTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);  // "Bearer " 이후의 부분을 반환
        }
        throw new OAuthAccessTokenNotFound("Access token is missing.");
    }

    public static String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        throw new OAuthTokenNotFoundException("Refresh token is missing.");
    }

    // 헤더에 AccessToken을 추가하는 메서드
    public static void addAccessTokenToHeader(HttpHeaders headers, String accessToken) {
        headers.add("Authorization", "Bearer " + accessToken);
    }

    // RefreshToken을 쿠키에 저장하는 메서드
    public static void setRefreshTokenToCookie(HttpServletResponse response, HttpHeaders headers, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);  // 보안 강화
        cookie.setPath("/");  // 경로 설정
        cookie.setMaxAge(60 * 60 * 24 * 30);  // 30일 동안 유효
        response.addCookie(cookie);

//        headers.add("Set-Cookie", "refresh_token=" + cookie.getValue() +
//                "; HttpOnly; Path=/; Max-Age=86400; SameSite=None");

        // 쿠키에 저장된 refreshToken 확인 (로그로 출력)
        System.out.println("MemberApiController : Refresh Token 쿠키에 저장됨: " + cookie.getValue() + "/" + cookie.getPath() + "/" + cookie.getMaxAge());
    }
}
