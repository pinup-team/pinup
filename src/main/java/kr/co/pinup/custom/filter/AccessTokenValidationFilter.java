package kr.co.pinup.custom.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class AccessTokenValidationFilter extends OncePerRequestFilter {

    private final SecurityUtil securityUtil;
    private final MemberService memberService;

    // 필터 적용 제외할 URL 목록
    private static final List<String> EXCLUDED_URLS = List.of(
            "/static/", "/templates/", "/css/", "/js/", "/images/", "/fonts/", "/error", "/favicon.ico",
            "/members/login", "/api/members/oauth/",
            "/notices", "/notices/", "/api/notices", "/api/notices/"
    );

    public AccessTokenValidationFilter(MemberService memberService, SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
        this.memberService = memberService;
    }

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
        return EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}