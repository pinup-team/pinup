package kr.co.pinup.security;

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

    public HttpSession getSession(boolean result) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.error("Failed to retrieve request attributes: No request attributes found.");
            throw new UnauthorizedException("No request attributes found.");
        }

        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(result);

        if (session == null) {
            log.error("No session found.");
            throw new UnauthorizedException("Session not found.");
        }

        return session;
    }

    public void setAuthentication(OAuthToken oAuthToken, MemberInfo memberInfo) {
        HttpSession session = getSession(true);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        authentication.setDetails(oAuthToken.getAccessToken());

        SecurityContextHolder.getContext().setAuthentication(authentication);

//        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
    }

    public Authentication getAuthentication() {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth == null || !currentAuth.isAuthenticated()) {
            throw new UnauthorizedException();
        }
        return currentAuth;
    }
//  TODO SecurityUtil getAuthentication() session으로 수정하고 추가하기
//    public Authentication getAuthentication() {
//        HttpSession session = getSession(false);
//
//        SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
//
//        Authentication currentAuth = (securityContext != null) ? securityContext.getAuthentication() : null;
//
//        if (currentAuth == null || !currentAuth.isAuthenticated()) {
//            throw new UnauthorizedException();
//        }
//        return currentAuth;
//    }

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
            log.error("로그인 정보가 없습니다.");
            throw new OAuth2AuthenticationException("로그인 정보가 없습니다.");
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
            log.error("MemberInfo가 없습니다.");
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
        cookie.setSecure(false); // TODO true로 변경
        cookie.setMaxAge(60 * 60 * 12);
        response.addCookie(cookie);
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
        cookie.setSecure(false); // TODO true로 변경
        response.addCookie(cookie);
    }

    public void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(false); // TODO true로 변경
        response.addCookie(cookie);
    }
}