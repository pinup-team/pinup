package kr.co.pinup.members.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.naver.NaverToken;
import kr.co.pinup.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
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
    private HttpSession session;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // Mock 객체 초기화
        mockMvc = MockMvcBuilders.standaloneSetup(securityUtil).build();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        memberInfo = new MemberInfo("네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        accessToken = "valid-access-token";

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @Nested
    @DisplayName("세션 테스트")
    class SessionTests {
        private MockHttpSession session = new MockHttpSession();

        @Test
        @DisplayName("세션이 존재할 경우 세션 반환")
        public void shouldReturnSession_WhenSessionExists() {
            when(request.getSession(true)).thenReturn(session);
            HttpSession retrievedSession = securityUtil.getSession(true);
            assertNotNull(retrievedSession);
            assertEquals(session, retrievedSession);
        }

        @Test
        @DisplayName("요청 속성이 null일 경우 UnauthorizedException 발생")
        public void shouldThrowUnauthorizedException_WhenRequestAttributesAreNull() {
            RequestContextHolder.setRequestAttributes(null);
            Exception exception = assertThrows(UnauthorizedException.class, () -> securityUtil.getSession(true));
            assertEquals("No request attributes found.", exception.getMessage());
        }

        @Test
        @DisplayName("세션이 null일 경우 UnauthorizedException 발생")
        public void shouldThrowUnauthorizedException_WhenSessionIsNull() {
            when(request.getSession(true)).thenReturn(null);
            Exception exception = assertThrows(UnauthorizedException.class, () -> securityUtil.getSession(true));
            assertEquals("Session not found.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("인증 테스트")
    class AuthenticationTests {
        @Test
        @DisplayName("인증 정보 설정")
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

            HttpSession session = mock(HttpSession.class);
            when(request.getSession(true)).thenReturn(session);

            securityUtil.setAuthentication(oAuthToken, memberInfo);

            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(currentAuth);
            assertEquals(memberInfo, (currentAuth.getPrincipal()));
            assertEquals(oAuthToken.getAccessToken(), currentAuth.getDetails());
        }

        @Test
        @WithMockMember
        @DisplayName("인증 정보 가져오기_성공")
        public void testGetAuthentication_Authenticated1() {
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
        @DisplayName("인증 정보 가져오기_성공")
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
        @DisplayName("인증 정보가 없을 경우 UnauthorizedException 발생")
        public void testGetAuthentication_Unauthenticated() {
            SecurityContextHolder.clearContext();

            when(request.getSession(false)).thenReturn(null);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            Exception exception = assertThrows(UnauthorizedException.class, securityUtil::getAuthentication);
            assertEquals("인증 정보가 없습니다.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("사용자 정보 테스트")
    class MemberInfoTests {
        @Test
        @DisplayName("회원 정보를 가져오기")
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
        @DisplayName("회원 정보가 없을 경우 OAuth2AuthenticationException 발생")
        public void testGetMemberInfo_NoMemberInfo() {
            SecurityContextHolder.clearContext(); // Clear context to simulate no member info
            Exception exception = assertThrows(OAuth2AuthenticationException.class, securityUtil::getMemberInfo);
            assertEquals("MemberInfo가 없습니다.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Refresh Token 테스트")
    class RefreshTokenTests {
        @Test
        @DisplayName("SecurityContext에서 액세스 토큰 갱신")
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
        @DisplayName("SecurityContext에서 액세스 토큰 가져오기")
        public void testGetAccessTokenFromSecurityContext() {
//        UsernamePasswordAuthenticationToken authentication =
//                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());
//        authentication.setDetails(accessToken);
//        SecurityContextHolder.getContext().setAuthentication(authentication);

            String retrievedAccessToken = securityUtil.getAccessTokenFromSecurityContext();
            assertEquals(accessToken, retrievedAccessToken);
        }
    }

    @Nested
    @DisplayName("Access Token 테스트")
    class AccessTokenTests {
        @Test
        @DisplayName("옵셔널 리프레시 토큰 가져오기")
        public void testGetOptionalRefreshToken() {
            Cookie cookie = new Cookie("refresh_token", "refreshTokenValue");
            when(request.getCookies()).thenReturn(new Cookie[]{cookie});

            String refreshToken = securityUtil.getOptionalRefreshToken(request);
            assertEquals("refreshTokenValue", refreshToken);
        }

        @Test
        @DisplayName("쿠키가 없을 경우 옵셔널 리프레시 토큰은 null")
        public void testGetOptionalRefreshToken_NoCookie() {
            when(request.getCookies()).thenReturn(null);

            String refreshToken = securityUtil.getOptionalRefreshToken(request);
            assertNull(refreshToken);
        }

        @Test
        @DisplayName("리프레시 토큰을 쿠키에 설정")
        public void testSetRefreshTokenToCookie() {
            securityUtil.setRefreshTokenToCookie(response, "refreshTokenValue");
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(response, times(1)).addCookie(cookieCaptor.capture());
            assertEquals("refresh_token", cookieCaptor.getValue().getName());
            assertEquals("refreshTokenValue", cookieCaptor.getValue().getValue());
        }
    }

    @Nested
    @DisplayName("삭제 테스트")
    class ClearTests {
        @Test
        @WithMockMember(nickname = "testUser", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        @DisplayName("컨텍스트를 지우고 쿠키 삭제")
        public void testClearContextAndDeleteCookie() {
            when(oAuthService.revoke(any(), any())).thenReturn(true); // Mocking the revoke method

            // 인증 정보를 설정한 후 메서드 호출
            securityUtil.clearContextAndDeleteCookie();

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

    /*@Test CHECK
    @WithMockMember(nickname = "testUser", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    public void testClearContextAndDeleteCookie_TokenRevokeFail() {
        *//*when(oAuthService.revoke(any(), any())).thenReturn(false);
        OAuthTokenRequestException exception = assertThrows(OAuthTokenRequestException.class, securityUtil::clearContextAndDeleteCookie);
        assertEquals("SecurityUtil clearContextAndDeleteCookie || Access Token 무효화에 실패했습니다.", exception.getMessage());*//*

        // 테스트를 위한 더미 데이터
        String accessToken = "dummyAccessToken";
        MemberInfo mockMemberInfo = mock(MemberInfo.class);
        when(mockMemberInfo.provider()).thenReturn(OAuthProvider.NAVER);

        // Mock의 동작 정의
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn(accessToken);
        when(securityUtil.getMemberInfo()).thenReturn(mockMemberInfo);
        when(oAuthService.revoke(any(), any())).thenReturn(false); // revoke가 false를 반환

        // 예외 발생 검증
        Exception exception = assertThrows(OAuthTokenRequestException.class, securityUtil::clearContextAndDeleteCookie);
        assertEquals("SecurityUtil clearContextAndDeleteCookie || Access Token 무효화에 실패했습니다.", exception.getMessage());
    }*/

        @Test
        @DisplayName("리프레시 토큰 쿠키 삭제")
        public void testClearRefreshTokenCookie() {
            securityUtil.clearRefreshTokenCookie(response);
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(response, times(1)).addCookie(cookieCaptor.capture());
            assertEquals("refresh_token", cookieCaptor.getValue().getName());
            assertNull(cookieCaptor.getValue().getValue());
            assertEquals(0, cookieCaptor.getValue().getMaxAge());
        }

        @Test
        @DisplayName("세션 쿠키 삭제")
        public void testClearSessionCookie() {
            securityUtil.clearSessionCookie(response);
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(response, times(1)).addCookie(cookieCaptor.capture());
            assertEquals("JSESSIONID", cookieCaptor.getValue().getName());
            assertNull(cookieCaptor.getValue().getValue());
            assertEquals(0, cookieCaptor.getValue().getMaxAge());
        }
    }
}
