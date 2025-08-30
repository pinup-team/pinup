package kr.co.pinup.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.security.SecurityConstants;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class AccessTokenValidationFilter extends OncePerRequestFilter {

    private final MemberService memberService;
    private final SecurityUtil securityUtil;
    private final AppLogger appLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // .well-known 및 appspecific 경로 차단
        if (requestURI.startsWith("/.well-known/") || requestURI.startsWith("/appspecific/")) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (isExcluded(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String accessToken = securityUtil.getAccessTokenFromSecurityContext();
            MemberInfo memberInfo = securityUtil.getMemberInfo();

            if (memberService.isAccessTokenExpired(memberInfo, accessToken) || accessToken == null) {
                appLogger.warn(new WarnLog("AccessTokenFilter AccessToken 만료되었거나 존재 X")
                        .addDetails("requestURI", requestURI));

                accessToken = memberService.refreshAccessToken(request);
                if (accessToken == null) {
                    appLogger.warn(new WarnLog("AccessTokenFilter AccessToken 재발급 실패")
                            .addDetails("requestURI", requestURI));
                    throw new OAuthTokenRequestException("Access Token Refresh Fail");
                } else {
                    securityUtil.refreshAccessTokenInSecurityContext(accessToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (UnauthorizedException e) {
            appLogger.error(new ErrorLog("AccessTokenFilter 사용자 인증 실패", e)
                    .addDetails("requestURI", requestURI));
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (OAuthAccessTokenNotFoundException e) {
            appLogger.error(new ErrorLog("AccessTokenFilter AccessToken 존재 X", e)
                    .addDetails("requestURI", requestURI));
            response.sendRedirect("/members/login");
        } catch (Exception e) {
            appLogger.error(new ErrorLog("AccessTokenFilter 예상치 못한 오류 발생", e)
                    .addDetails("requestURI", requestURI));
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }

    private boolean isExcluded(String requestURI) {
        if (requestURI.equals("/")) {
            return true;
        }
        return SecurityConstants.EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}