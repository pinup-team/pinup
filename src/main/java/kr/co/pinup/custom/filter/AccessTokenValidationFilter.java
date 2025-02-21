package kr.co.pinup.custom.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
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

        // 필터 제외 URL이면 바로 다음 필터로 진행
        if (isExcluded(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        log.info("AccessTokenValidationFilter");
        log.debug("URI is not excluded : {}", requestURI);

        try {
            String accessToken = securityUtil.getAccessTokenFromSecurityContext();
            MemberInfo memberInfo = securityUtil.getMemberInfo();
//        String accessToken = request.getHeader("Authorization");

            // 액세스 토큰 만료 여부 확인
            System.out.println("access token expired checking start");
            if (memberService.isAccessTokenExpired(memberInfo, accessToken)) {
                System.out.println("access token is expired");
                accessToken = memberService.refreshAccessToken(request);
                securityUtil.refreshAccessTokenInSecurityContext(accessToken);
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
            return true; // "/" 요청은 필터를 통과하도록 설정
        }
        return EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}