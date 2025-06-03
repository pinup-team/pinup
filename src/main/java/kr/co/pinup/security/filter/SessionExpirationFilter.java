package kr.co.pinup.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.security.SecurityConstants;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SessionExpirationFilter extends OncePerRequestFilter {

    private static final PathMatcher pathMatcher = new AntPathMatcher();
    private final SecurityUtil securityUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // .well-known 및 appspecific 경로 차단
//        if (requestURI.startsWith("/.well-known/") || requestURI.startsWith("/appspecific/")) {
//            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            return;
//        }

        // MDC
        try {
            MDC.put("requestId", getRequestId(request));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof final MemberInfo memberInfo) {
                MDC.put("nickname", memberInfo.nickname());
            }

            log.info("MDC get requestId: {}", MDC.get("requestId"));
            log.info("MDC get nickname: {}", MDC.get("nickname"));
        } finally {
            MDC.clear();
        }

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

    private String getRequestId(HttpServletRequest request) {
        String header = request.getHeader("X-Request-ID");
        return (header != null && !header.isEmpty()) ? header : UUID.randomUUID().toString();
    }
}
