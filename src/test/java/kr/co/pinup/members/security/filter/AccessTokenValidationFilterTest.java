package kr.co.pinup.members.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.security.filter.sub.TestAccessTokenValidationFilter;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class AccessTokenValidationFilterTest {

    private TestAccessTokenValidationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private MemberService memberService;
    private SecurityUtil securityUtil;
    private AppLogger appLogger;

    @BeforeEach
    void setUp() {
        memberService = Mockito.mock(MemberService.class);
        securityUtil = Mockito.mock(SecurityUtil.class);
        appLogger = Mockito.mock(AppLogger.class);
        filter = new TestAccessTokenValidationFilter(memberService, securityUtil, appLogger);

        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        chain = Mockito.mock(FilterChain.class);
    }

    @Test
    void testDoFilterInternal_WhenExcludedUrl_ShouldProceedToFilterChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/static/test");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WhenAccessTokenIsExpired_ShouldRefreshToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/protected");
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("expired_token");
        when(securityUtil.getMemberInfo()).thenReturn(Mockito.mock(MemberInfo.class));
        when(memberService.isAccessTokenExpired(any(), any())).thenReturn(true);
        when(memberService.refreshAccessToken(request)).thenReturn("new_access_token");

        filter.doFilterInternal(request, response, chain);

        verify(memberService).refreshAccessToken(request);
        verify(securityUtil).refreshAccessTokenInSecurityContext("new_access_token");
        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WhenAccessTokenRefreshFails_ShouldThrowException() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/protected");
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("expired_token");
        when(securityUtil.getMemberInfo()).thenReturn(Mockito.mock(MemberInfo.class));
        when(memberService.isAccessTokenExpired(any(), any())).thenReturn(true);
        when(memberService.refreshAccessToken(request)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }
}