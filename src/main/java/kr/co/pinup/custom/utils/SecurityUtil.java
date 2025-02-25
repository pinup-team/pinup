package kr.co.pinup.custom.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class SecurityUtil {

    private static OAuthService oAuthService;

    @Autowired
    public void setOAuthService(OAuthService oAuthService) {
        SecurityUtil.oAuthService = oAuthService;
    }
    /*private final OAuthService oAuthService;

    @Autowired
    public SecurityUtil(OAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }*/

    public void setAuthentication(OAuthToken oAuthToken, MemberInfo memberInfo) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        authentication.setDetails(oAuthToken.getAccessToken());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public Authentication getAuthentication() {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth == null || !currentAuth.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        return currentAuth;
    }

    public void setMemberInfo(MemberInfo newMemberInfo) {
        try {
            Authentication currentAuth = getAuthentication();

            MemberInfo memberInfo = (MemberInfo) currentAuth.getPrincipal();
            if (memberInfo == null) {
                log.error("MemberInfo doesn't exist!");
                throw new OAuth2AuthenticationException();
            } else {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(newMemberInfo, null, newMemberInfo.getAuthorities());

                authentication.setDetails(currentAuth.getDetails());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (UnauthorizedException e) {
            log.error("MemberInfo doesn't exist!");
            throw new OAuth2AuthenticationException("새로운 MemberInfo가 없습니다.");
        }
    }

    public MemberInfo getMemberInfo() {
        try {
            Authentication currentAuth = getAuthentication();

            MemberInfo memberInfo = (MemberInfo) currentAuth.getPrincipal();
            if (memberInfo == null) {
                log.error("MemberInfo doesn't exist!");
                throw new OAuth2AuthenticationException();
            }
            return memberInfo;
        } catch (UnauthorizedException e) {
            log.error("MemberInfo doesn't exist!");
            throw new OAuth2AuthenticationException("MemberInfo가 없습니다.");
        }
    }

    public void refreshAccessTokenInSecurityContext(String accessToken) {
        Authentication currentAuth = getAuthentication();

        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                currentAuth.getPrincipal(),
                currentAuth.getCredentials(),
                currentAuth.getAuthorities()
        );

        newAuth.setDetails(accessToken);
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    public String getAccessTokenFromSecurityContext() {
        Authentication currentAuth = getAuthentication();
        if (currentAuth.isAuthenticated()) {
            log.debug("SecurityUtil getAccessTokenFromSecurityContext authenticated : {}", currentAuth.getDetails());
            return (String) currentAuth.getDetails();
        }
        return null;
    }

    public String getOptionalRefreshToken(HttpServletRequest request) {
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

    public void setRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setMaxAge(60 * 60 * 12);
        response.addCookie(cookie);
        log.debug("SecurityUtil : Refresh Token 쿠키에 저장됨: {}", cookie.getValue());
    }

    public void clearContextAndDeleteCookie() {
        try {
            MemberInfo memberInfo = getMemberInfo();
            String accessToken = getAccessTokenFromSecurityContext();
            if (accessToken != null) {
                if (!oAuthService.revoke(memberInfo.provider(), accessToken)) {
                    throw new OAuthTokenRequestException("SecurityUtil clearContextAndDeleteCookie || Access Token 무효화에 실패했습니다.");
                }
            }
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
            log.debug("SecurityUtil : clearContextAndDeleteCookie 성공");
        } catch (UnauthorizedException e) {
            log.error("SecurityUtil clearContextAndDeleteCookie || Access Token 무효화에 실패했습니다.");
            throw new OAuthTokenRequestException("SecurityUtil clearContextAndDeleteCookie || Access Token 무효화에 실패했습니다.");
        }
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(false);
        response.addCookie(cookie);
        log.debug("Refresh Token 쿠키 삭제 완료");
    }

    public void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(false);  // HTTPS 환경이라면 true 설정
        response.addCookie(cookie);
        log.debug("JSESSIONID 쿠키 삭제 완료");
    }

    /*// 헤더에 AccessToken을 추가하는 메서드
    public void addAccessTokenToHeader(HttpHeaders headers, String accessToken) {
        headers.add("Authorization", "Bearer " + accessToken);
    }

    public String getAccessTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            System.out.println("AccessToken from header: " + accessToken);
            return accessToken;
        }
        throw new OAuthTokenNotFoundException("Access token is missing in header.");
    }

    public String getCurrentUsername() {
        Authentication currentAuth = getAuthentication();
        if (currentAuth.isAuthenticated()) {
            return currentAuth.getName();  // 현재 인증된 사용자의 이름 반환
        } else throw new UnauthorizedException("SecurityUtil getCurrentUsername : 인증 정보가 없습니다.");
    }*/
}