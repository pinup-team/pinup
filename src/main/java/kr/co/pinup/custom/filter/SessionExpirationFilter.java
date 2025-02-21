package kr.co.pinup.custom.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
@Slf4j
public class SessionExpirationFilter extends OncePerRequestFilter {

    /*@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            SecurityUtil.getAuthentication();
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException e) {
            SecurityUtil.clearContextAndDeleteCookie();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인 정보가 없습니다.");
        }
    }*/
    // CHECK 지금 모든 API에 적용중
    // 필터 적용을 제외할 URL 목록
    private static final List<String> EXCLUDED_URLS = List.of(
            "/static/", "/templates/", "/css/", "/js/", "/images/", "/fonts/", "/error", "/favicon.ico",
            "/members/login", "/api/members/oauth/",
            "/notices", "/notices/", "/api/notices", "/api/notices/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 예외 처리: 특정 URL 패턴은 필터 적용 제외
        if (isExcluded(requestURI)) {
            chain.doFilter(request, response);
            return;
        }
        log.info("SessionExpirationFilter");
        log.debug("URI is not excluded : {}", requestURI);

        // 세션 만료 여부 확인
        if (request.getSession(false) == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExcluded(String requestURI) {
        if (requestURI.equals("/")) {
            return true; // "/" 요청은 필터를 통과하도록 설정
        }
        return EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}
