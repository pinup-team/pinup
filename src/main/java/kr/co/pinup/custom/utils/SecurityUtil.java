package kr.co.pinup.custom.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthTokenNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.oauth.OAuthToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// TODO 체크하기
@Slf4j
public class SecurityUtil {
    public static void setAuthentication(OAuthToken oAuthToken, MemberInfo memberInfo) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        authentication.setDetails(oAuthToken.getAccessToken()); // `accessToken`을 details에 설정

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static Authentication getAuthentication() {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null && currentAuth.isAuthenticated()) {
            System.out.println("SecurityUtil Authentication 성공: " + currentAuth.getName());
            // 권한 확인
            System.out.println("권한 정보: " + currentAuth.getAuthorities());
            return currentAuth;
        } else {
            log.error("SecurityUtil Authentication 실패");
            throw new UnauthorizedException();
        }
    }

    public static MemberInfo getMemberInfo() {
        Authentication currentAuth = getAuthentication();

        MemberInfo memberInfo = (MemberInfo) currentAuth.getPrincipal();
        if (memberInfo == null) {
            log.error("MemberInfo doesn't exist!");
            throw new OAuth2AuthenticationException();
        }
        return memberInfo;
    }

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

    public static void refreshAccessTokenInSecurityContext(String accessToken) {
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
            throw new UnauthorizedException("SecurityUtil refreshAccessTokenInSecurityContext : 인증 정보가 없습니다.");
        }
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();  // 현재 인증된 사용자의 이름 반환
        } else throw new UnauthorizedException("SecurityUtil getCurrentUsername : 인증 정보가 없습니다.");
    }

    public static String getAccessTokenFromSecurityContext() {
        System.out.println("SecurityUtil getAccessTokenFromSecurityContext");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) { // SecurityContext가 비어있지 않고, 인증된 사용자일 경우
            System.out.println("SecurityUtil getAccessTokenFromSecurityContext authenticated : " + authentication.getDetails());
            return (String) authentication.getDetails(); // accessToken이 details에 저장되었을 경우 반환
        } // TODO 이거 생각한 대로 진행이 안됨..refresh가 사라져도 securitycontext에는 남아잇어야하느데 securitycontext에 있는 accesstoken도 같이 사라져버림? 이상하다
        return null;
    }

    public static String getOptionalRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static void setRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setMaxAge(60 * 60 * 12);
        response.addCookie(cookie);
        System.out.println("SecurityUtil : Refresh Token 쿠키에 저장됨: " + cookie.getValue());
    }

    public static void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(false);
        response.addCookie(cookie);
        log.debug("Refresh Token 쿠키 삭제 완료");
    }

    public static void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(false);  // HTTPS 환경이라면 true 설정
        response.addCookie(cookie);
        log.debug("JSESSIONID 쿠키 삭제 완료");
    }

    // TODO logout할때 호출될 clearContextAndDeleteCookie
    public static void clearContextAndDeleteCookie() {
        System.out.println("SecurityUtil clearContextAndDeleteCookie");

        SecurityContextHolder.clearContext();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            HttpServletResponse response = attributes.getResponse();

            if (response != null) {
                clearRefreshTokenCookie(response);
                clearSessionCookie(response);
            }

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }
    }
}