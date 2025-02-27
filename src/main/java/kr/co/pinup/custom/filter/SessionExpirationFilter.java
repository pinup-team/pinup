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

    private static final List<String> EXCLUDED_URLS = List.of(
            "/static/", "/templates/", "/css/", "/js/", "/images/", "/fonts/", "/error", "/favicon.ico",
            "/members/login", "/api/members/oauth/",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (isExcluded(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        if (request.getSession(false) == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "SessionExpirationFilter : Session expired");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExcluded(String requestURI) {
        if (requestURI.equals("/")) {
            return true;
        }
        return EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}
