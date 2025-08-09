package kr.co.pinup.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class SecurityUtil {

    private static OAuthService oAuthService;

    @Value("${cookie.secure}")
    private String cookieSecure;

    private boolean cookieSetting;

    @Autowired
    public void setOAuthService(OAuthService oAuthService) {
        SecurityUtil.oAuthService = oAuthService;
    }

    @PostConstruct
    private void init() {
        // YAML 에 N 이면 false, 그 외에는 true
        this.cookieSetting = !"N".equalsIgnoreCase(cookieSecure);
        log.info("cookie.secure='{}' → cookieSetting={}", cookieSecure, cookieSetting);
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
            if (accessToken != null && memberInfo.getProvider() != OAuthProvider.PINUP) {
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
        cookie.setSecure(cookieSetting);
        response.addCookie(cookie);
    }

    public void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(cookieSetting);
        response.addCookie(cookie);
    }

    public String generateToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + Duration.ofHours(3).toMillis()); // 1시간 유효

        String dynamicSecretKey = java.util.UUID.randomUUID().toString().replace("-", "");

        return Jwts.builder()
                .setSubject(member.getEmail())  // 토큰 주체, 보통 userId 혹은 email
                .claim("role", member.getRole().name()) // 사용자 역할 권한 등 추가 정보
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, dynamicSecretKey.getBytes()) // secretKey를 바이트 배열로 전달
                .compact();
    }

}