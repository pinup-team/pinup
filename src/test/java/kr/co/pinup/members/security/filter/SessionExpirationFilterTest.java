package kr.co.pinup.members.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.members.security.filter.sub.TestSessionExpirationFilter;
import kr.co.pinup.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class SessionExpirationFilterTest {

    private TestSessionExpirationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private SecurityUtil securityUtil;

    @BeforeEach
    void setUp() {
        securityUtil = Mockito.mock(SecurityUtil.class);
        filter = new TestSessionExpirationFilter(securityUtil);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        chain = Mockito.mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/members/oauth/login");
    }

    @Test
    void testDoFilterInternal_WhenSessionIsNull_ShouldReturnUnauthorized() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/members");
        when(request.getSession(false)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "SessionExpirationFilter : Session expired");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WhenSessionIsValid_ShouldProceedToFilterChain() throws Exception {
        when(request.getSession(false)).thenReturn(Mockito.mock(HttpSession.class));

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
