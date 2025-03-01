package kr.co.pinup.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.security.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
@Slf4j
public class SessionExpirationFilter extends OncePerRequestFilter {

    private static final PathMatcher pathMatcher = new AntPathMatcher();

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
        // Ant-style 경로 매칭을 위해 PathMatcher 사용
        return Arrays.stream(SecurityConstants.PUBLIC_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
    }
}
