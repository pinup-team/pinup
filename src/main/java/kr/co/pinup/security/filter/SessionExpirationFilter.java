package kr.co.pinup.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.security.SecurityConstants;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class SessionExpirationFilter extends OncePerRequestFilter {

    private final SecurityUtil securityUtil;

    @Autowired
    private AppLogger appLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (isExcluded(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (request.getSession(false) == null) {
                appLogger.warn(new WarnLog("SessionExpirationFilter: 세션이 만료되었거나 존재하지 않음")
                        .addDetails("requestURI", requestURI));
                securityUtil.clearRefreshTokenCookie(response);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "SessionExpirationFilter : Session expired");
                return;
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            appLogger.error(new ErrorLog("SessionExpirationFilter: 예상치 못한 예외 발생", e)
                    .addDetails("requestURI", requestURI));
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SessionExpirationFilter: Unexpected error");
        }
    }

    private boolean isExcluded(String requestURI) {
        if (requestURI.equals("/")) {
            return true;
        }
        return SecurityConstants.EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}