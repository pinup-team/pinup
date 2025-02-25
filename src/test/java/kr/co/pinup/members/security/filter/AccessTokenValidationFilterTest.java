package kr.co.pinup.members.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.security.filter.sub.TestAccessTokenValidationFilter;
import kr.co.pinup.members.service.MemberService;
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

    @BeforeEach
    void setUp() {
        memberService = Mockito.mock(MemberService.class);
        securityUtil = Mockito.mock(SecurityUtil.class);
        filter = new TestAccessTokenValidationFilter(memberService, securityUtil);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        chain = Mockito.mock(FilterChain.class);
    }

    @Test
    void testDoFilterInternal_WhenExcludedUrl_ShouldProceedToFilterChain() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/static/test");

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(chain).doFilter(request, response); // 체인이 호출되어야 함
    }

    @Test
    void testDoFilterInternal_WhenAccessTokenIsExpired_ShouldRefreshToken() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/protected");
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("expired_token");
        when(securityUtil.getMemberInfo()).thenReturn(Mockito.mock(MemberInfo.class));
        when(memberService.isAccessTokenExpired(any(), any())).thenReturn(true);
        when(memberService.refreshAccessToken(request)).thenReturn("new_access_token");

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(memberService).refreshAccessToken(request); // 액세스 토큰 새로 고침 메서드 호출 확인
        verify(securityUtil).refreshAccessTokenInSecurityContext("new_access_token"); // 새 토큰이 보안 컨텍스트에 설정되어야 함
        verify(chain).doFilter(request, response); // 체인이 호출되어야 함
    }

    @Test
    void testDoFilterInternal_WhenAccessTokenRefreshFails_ShouldThrowException() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/protected");
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("expired_token");
        when(securityUtil.getMemberInfo()).thenReturn(Mockito.mock(MemberInfo.class));
        when(memberService.isAccessTokenExpired(any(), any())).thenReturn(true);
        when(memberService.refreshAccessToken(request)).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, chain);

        // Then
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred."); // 에러가 발생해야 함
    }
}