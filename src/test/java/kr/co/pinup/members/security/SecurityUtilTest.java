package kr.co.pinup.members.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.custom.MemberTestAnnotation;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.naver.NaverToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

//@MemberTestAnnotation
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SecurityUtilTest {
    MockMvc mockMvc;

    @Mock
    private OAuthService oAuthService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private SecurityUtil securityUtil;

    private MemberInfo memberInfo;
    private String accessToken;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(securityUtil).build();

        // Mock MemberInfo
        memberInfo = new MemberInfo("네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        accessToken = "valid-access-token";

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @Test
    @WithMockMember
    public void testSetAuthentication() {
        SecurityContextHolder.clearContext();
        OAuthToken oAuthToken = NaverToken.builder()
                .accessToken(accessToken)
                .refreshToken("valid-refresh-token")
                .tokenType("test-token-type")
                .expiresIn(1000)
                .result("success")
                .error("test-error")
                .errorDescription("test-error-description")
                .build();
        securityUtil.setAuthentication(oAuthToken, memberInfo);

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(currentAuth);
        assertEquals(memberInfo, (currentAuth.getPrincipal()));
        assertEquals(oAuthToken.getAccessToken(), currentAuth.getDetails());
    }

    @Test
    public void testGetAuthentication_Authenticated() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        authentication.setDetails(accessToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Authentication auth = securityUtil.getAuthentication();
        assertNotNull(auth);
        assertEquals("네이버TestMember", ((MemberInfo) auth.getPrincipal()).nickname());
    }

    @Test
    @WithMockMember
    public void testGetAuthentication_Unauthenticated() {
        SecurityContextHolder.clearContext();
        Exception exception = assertThrows(UnauthorizedException.class, securityUtil::getAuthentication);
        assertEquals("인증 정보가 없습니다.", exception.getMessage());
    }

    @Test
    public void testGetMemberInfo() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        authentication.setDetails(accessToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        MemberInfo info = securityUtil.getMemberInfo();
        assertEquals("네이버TestMember", info.nickname());
    }

    @Test
    @WithMockMember
    public void testGetMemberInfo_NoMemberInfo() {
        SecurityContextHolder.clearContext(); // Clear context to simulate no member info
        Exception exception = assertThrows(OAuth2AuthenticationException.class, securityUtil::getMemberInfo);
        assertEquals("MemberInfo가 없습니다.", exception.getMessage());
    }

    @Test
    public void testRefreshAccessTokenInSecurityContext() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        authentication.setDetails(accessToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String newAccessToken = "newAccessToken";
        securityUtil.refreshAccessTokenInSecurityContext(newAccessToken);
        String accessToken = securityUtil.getAccessTokenFromSecurityContext();
        assertEquals(newAccessToken, accessToken);
    }

    @Test
    @WithMockMember
    public void testGetAccessTokenFromSecurityContext() {
//        UsernamePasswordAuthenticationToken authentication =
//                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());
//        authentication.setDetails(accessToken);
//        SecurityContextHolder.getContext().setAuthentication(authentication);

        String retrievedAccessToken = securityUtil.getAccessTokenFromSecurityContext();
        assertEquals(accessToken, retrievedAccessToken);
    }

    @Test
    public void testGetOptionalRefreshToken() {
        Cookie cookie = new Cookie("refresh_token", "refreshTokenValue");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        String refreshToken = securityUtil.getOptionalRefreshToken(request);
        assertEquals("refreshTokenValue", refreshToken);
    }

    @Test
    public void testGetOptionalRefreshToken_NoCookie() {
        when(request.getCookies()).thenReturn(null);

        String refreshToken = securityUtil.getOptionalRefreshToken(request);
        assertNull(refreshToken);
    }

    @Test
    public void testSetRefreshTokenToCookie() {
        securityUtil.setRefreshTokenToCookie(response, "refreshTokenValue");
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(1)).addCookie(cookieCaptor.capture());
        assertEquals("refresh_token", cookieCaptor.getValue().getName());
        assertEquals("refreshTokenValue", cookieCaptor.getValue().getValue());
    }

    // TODO COOKIES 처리
    @Test
    @WithMockMember(nickname = "testUser", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    public void testClearContextAndDeleteCookie() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        authentication.setDetails(accessToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn(accessToken);
        when(oAuthService.revoke(any(), any())).thenReturn(true); // Mocking the revoke method

        // 인증 정보를 설정한 후 메서드 호출
        securityUtil.clearContextAndDeleteCookie();

        // Verify that revoke was called with expected parameters
        verify(oAuthService, times(1)).revoke(any(), eq(accessToken));

        // Optionally check if the context is cleared
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @WithMockMember
    public void testClearContextAndDeleteCookie_TokenRevokeFail() {
        when(oAuthService.revoke(any(), any())).thenReturn(false);

        Exception exception = assertThrows(OAuthTokenRequestException.class, securityUtil::clearContextAndDeleteCookie);
        assertEquals("SecurityUtil clearContextAndDeleteCookie || Access Token 무효화에 실패했습니다.", exception.getMessage());
    }

    @Test
    public void testClearRefreshTokenCookie() {
        securityUtil.clearRefreshTokenCookie(response);
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(1)).addCookie(cookieCaptor.capture());
        assertEquals("refresh_token", cookieCaptor.getValue().getName());
        assertNull(cookieCaptor.getValue().getValue());
        assertEquals(0, cookieCaptor.getValue().getMaxAge());
    }

    @Test
    public void testClearSessionCookie() {
        securityUtil.clearSessionCookie(response);
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, times(1)).addCookie(cookieCaptor.capture());
        assertEquals("JSESSIONID", cookieCaptor.getValue().getName());
        assertNull(cookieCaptor.getValue().getValue());
        assertEquals(0, cookieCaptor.getValue().getMaxAge());
    }
}
