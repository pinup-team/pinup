package kr.co.pinup.custom.mdc;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put("timestamp", Instant.now().toString());
            MDC.put("requestId", getRequestId(request));
            MDC.put("userNickName", getUserNickName().orElse("anonymous"));

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getRequestId(HttpServletRequest request) {
        String header = request.getHeader("X-Request-ID");
        return (header != null && !header.isEmpty()) ? header : UUID.randomUUID().toString();
    }

    private Optional<String> getUserNickName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
            return Optional.ofNullable(authentication.getName());
        }
        return Optional.empty();
    }
}