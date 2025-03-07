package kr.co.pinup.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.security.SecurityConstants;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Slf4j
@RequiredArgsConstructor
public class SessionExpirationFilter extends OncePerRequestFilter {

    private static final PathMatcher pathMatcher = new AntPathMatcher();
    private final SecurityUtil securityUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (isExcluded(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        if (request.getSession(false) == null) {
            securityUtil.clearRefreshTokenCookie(response);
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
//        return Arrays.stream(SecurityConstants.PUBLIC_URLS)
//                .anyMatch(pattern -> pathMatcher.match(pattern, requestURI));
        return SecurityConstants.EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}
