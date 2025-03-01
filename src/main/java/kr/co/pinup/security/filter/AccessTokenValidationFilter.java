package kr.co.pinup.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.security.SecurityConstants;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class AccessTokenValidationFilter extends OncePerRequestFilter {

    private final MemberService memberService;
    private final SecurityUtil securityUtil;
    private static final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (isExcluded(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = securityUtil.getAccessTokenFromSecurityContext();
            MemberInfo memberInfo = securityUtil.getMemberInfo();

            if (memberService.isAccessTokenExpired(memberInfo, accessToken) || accessToken == null) {
                log.warn("AccessTokenValidationFilter : Access Token is expired or null");
                accessToken = memberService.refreshAccessToken(request);
                if(accessToken == null){
                    log.error("AccessTokenValidationFilter : Access Token Refresh Fail");
                    throw new OAuthTokenRequestException("Access Token Refresh Fail");
                } else {
                    log.debug("AccessTokenValidationFilter : Access Token Refresh Success");
                    securityUtil.refreshAccessTokenInSecurityContext(accessToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (UnauthorizedException e) {
            log.error("AccessTokenValidationFilter || UnauthorizedException: {}", e.getMessage());
        } catch (OAuthAccessTokenNotFoundException e) {
            log.error("AccessTokenValidationFilter || Access token not found: {}", e.getMessage());
            response.sendRedirect("/login"); // 로그인 페이지로 리다이렉트
        } catch (Exception e) {
            log.error("AccessTokenValidationFilter || Unexpected error: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    private boolean isExcluded(String requestURI) {
        if (requestURI.equals("/")) {
            return true;
        }
        // Ant-style 경로 매칭을 위해 PathMatcher 사용
        return Arrays.stream(SecurityConstants.PUBLIC_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }
}