package kr.co.pinup.members.oauth;

import jakarta.servlet.http.Cookie;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.exception.*;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.oauth.OAuthToken;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.oauth.naver.NaverResponse;
import kr.co.pinup.oauth.naver.NaverToken;
import kr.co.pinup.security.SecurityUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class NaverOAuthTest {
    MockMvc mockMvc;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OAuthService oAuthService;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private MemberInfo memberInfo;
    private NaverResponse naverResponse;
    private NaverToken naverToken;
    private Pair<OAuthResponse, OAuthToken> naverPair;

    private NaverLoginParams params;
    private GoogleLoginParams errorParams;

    private String accessToken = "valid-access-token";
    private String invalidAccessToken = "invalid-access-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberService).build();

        member = Member.builder()
                .name("testUser")
                .email("test@naver.com")
                .nickname("네이버TestMember")
                .providerType(OAuthProvider.NAVER)
                .providerId("testId123456789")
                .role(MemberRole.ROLE_USER)
                .build();
        memberInfo = MemberInfo.builder()
                .nickname("네이버TestMember")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();

        params = NaverLoginParams.builder().code("test-code").state("test-state").build();
        errorParams = GoogleLoginParams.builder().error("access_denied").build();

        naverResponse = NaverResponse.builder().response(NaverResponse.Response.builder().id("testId123456789").name("testUser").email("test@naver.com").build()).build();
        naverToken = NaverToken.builder().accessToken("valid-access-token").refreshToken("valid-refresh-token").tokenType("test-token-type").expiresIn(1000).result("success").error("test-error").errorDescription("test-error-description").build();
        naverPair = Pair.of(naverResponse, naverToken);

        // Mock 객체의 동작 정의
        doNothing().when(securityUtil).setAuthentication(naverToken, memberInfo);
    }

    @Nested
    @DisplayName("NAVER 로그인/회원가입 관련 테스트")
    class LoginMemberTests {
        private MockHttpSession session = new MockHttpSession();

        @Test
        @WithMockMember
        @DisplayName("OAuth 로그인 성공")
        void login_Success() {
            when(oAuthService.request(any())).thenReturn(naverPair);
            when(memberRepository.findByEmail(any())).thenReturn(Optional.ofNullable(member));
            doNothing().when(securityUtil).setAuthentication(any(), any());

            Pair<OAuthResponse, OAuthToken> result = memberService.login(params, session);
            OAuthResponse oAuthResponse = result.getLeft();

            assertNotNull(result);
            assertEquals(member.getName(), oAuthResponse.getName());
            assertEquals(member.getEmail(), oAuthResponse.getEmail());
            assertEquals(member.getProviderId(), oAuthResponse.getId());
            assertEquals(member.getProviderType(), oAuthResponse.getOAuthProvider());
            verify(oAuthService).request(any());
            verify(memberRepository).findByEmail(anyString());

            SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(securityContext);
            assertNotNull(securityContext.getAuthentication());
            assertEquals(memberInfo, securityContext.getAuthentication().getPrincipal());
        }

        @Test
        @DisplayName("로그인 실패_회원 정보 없음_회원가입 발생")
        void testLogin_WhenMemberNotFound_ShouldCreateNewMember() {
            when(oAuthService.request(any())).thenReturn(naverPair); // OAuth 응답 설정
            when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty()); // 회원 정보가 없을 때
            when(memberRepository.save(any(Member.class))).thenReturn(member); // 새로운 회원 저장

            Pair<OAuthResponse, OAuthToken> result = memberService.login(params, session);
            OAuthResponse oAuthResponse = result.getLeft();

            assertNotNull(result);
            assertEquals(member.getName(), oAuthResponse.getName());
            assertEquals(member.getEmail(), oAuthResponse.getEmail());
            assertEquals(member.getProviderId(), oAuthResponse.getId());
            assertEquals(member.getProviderType(), oAuthResponse.getOAuthProvider());
            verify(memberRepository).findByEmail(anyString()); // 이메일로 회원 조회
            verify(memberRepository).save(any(Member.class)); // 새 회원 저장이 호출되었는지 확인
        }

        @Test
        @DisplayName("로그인 실패_사용자 취소")
        void testLogin_WhenOAuthRequestFails_ShouldThrowOAuthLoginCanceledException() {
            when(oAuthService.request(any())).thenThrow(new OAuthLoginCanceledException("로그인을 취소합니다."));

            assertThrows(OAuthLoginCanceledException.class, () -> {
                memberService.login(errorParams, session);
            });
        }

        @Test
        @DisplayName("로그인 실패_OAuth 요청 실패")
        void testLogin_WhenOAuthRequestFails_ShouldThrowUnauthorizedException() {
            when(oAuthService.request(any())).thenThrow(new UnauthorizedException("Invalid OAuth request"));

            assertThrows(UnauthorizedException.class, () -> {
                memberService.login(params, session);
            });
        }
    }

    @Nested
    @DisplayName("Naver 로그아웃 관련 테스트")
    class LogOutMemberTests {
        private MockHttpServletRequest request = new MockHttpServletRequest();
        private OAuthProvider oAuthProvider = OAuthProvider.NAVER;

        @Test
        @DisplayName("로그아웃 성공")
        void testLogout_Success() {
            when(oAuthService.revoke(any(), any())).thenReturn(true);

            boolean result = memberService.logout(oAuthProvider, accessToken);

            assertTrue(result);
            assertNull(request.getSession(false));  // 세션에서 accessToken 제거 확인
            assertNull(SecurityContextHolder.getContext().getAuthentication());  // 인증 정보 삭제 확인
        }

        @Test
        @DisplayName("OAuth 제공자 없음")
        void testLogout_WhenOAuthProviderNotFound_ShouldThrowOAuthProviderNotFoundException() {
            OAuthProvider invalidOAuthProvider = null;

            assertThrows(OAuthProviderNotFoundException.class, () -> {
                memberService.logout(invalidOAuthProvider, accessToken);
            });
        }

        @Test
        @DisplayName("세션이 존재하지 않음")
        void testLogout_WhenSessionNotFound_ShouldThrowUnauthorizedException() {
            OAuthTokenNotFoundException exception = assertThrows(OAuthTokenNotFoundException.class, () -> {
                memberService.logout(oAuthProvider, null);
            });
            assertEquals("MemberService logout || OAuth Access 토큰을 찾을 수 없습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("accessToken 없음")
        void testLogout_WhenAccessTokenNotFound_ShouldThrowOAuthTokenNotFoundException() {
            assertThrows(OAuthTokenNotFoundException.class, () -> {
                memberService.logout(oAuthProvider, null);
            });
        }

        @Test
        @DisplayName("OAuth 로그아웃 실패")
        void testLogout_WhenOAuthRevokeFails_ShouldThrowOAuth2AuthenticationException() {
            when(oAuthService.revoke(any(), any())).thenReturn(false);
            when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn(accessToken);
            doThrow(new OAuthTokenRequestException("SecurityUtil clearContextAndDeleteCookie || Access Token 무효화에 실패했습니다."))
                    .when(securityUtil).clearContextAndDeleteCookie();

            assertThrows(OAuth2AuthenticationException.class, () -> {
                memberService.logout(oAuthProvider, accessToken);
            });
        }
    }

    @Nested
    @DisplayName("Access Token 관련 테스트")
    class AccessTokenTests {

        private String expiredAccessToken = "expired-access-token";
        private String refreshToken = "valid-refresh-token";

        @Test
        @DisplayName("Access Token 만료 확인 - 만료된 경우")
        void testIsAccessTokenExpired_WhenTokenIsExpired() {
            MemberInfo testMemberInfo = MemberInfo.builder()
                    .nickname("구글TestMember")
                    .provider(OAuthProvider.NAVER)
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(oAuthService.isAccessTokenExpired(testMemberInfo.provider(), expiredAccessToken))
                    .thenThrow(new OAuthAccessTokenNotFoundException("엑세스 토큰이 만료되었습니다."));

            boolean result = memberService.isAccessTokenExpired(testMemberInfo, expiredAccessToken);
            assertTrue(result);
        }

        @Test
        @DisplayName("Access Token 만료 확인 - 만료되지 않은 경우")
        void testIsAccessTokenExpired_WhenTokenIsNotExpired() {
            when(oAuthService.isAccessTokenExpired(memberInfo.provider(), accessToken))
                    .thenReturn(naverResponse);

            when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(member));

            boolean result = memberService.isAccessTokenExpired(memberInfo, accessToken);

            assertFalse(result); // 만료되지 않은 경우 false 반환
            verify(oAuthService).isAccessTokenExpired(memberInfo.provider(), accessToken);
            verify(memberRepository).findByEmail(anyString());
        }

        @Test
        @DisplayName("Access Token 만료 확인 - 예외 발생 시")
        void testIsAccessTokenExpired_WhenExceptionOccurs() {
            MemberInfo testMemberInfo = MemberInfo.builder()
                    .nickname("구글TestMember")
                    .provider(OAuthProvider.NAVER)
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(oAuthService.isAccessTokenExpired(testMemberInfo.provider(), accessToken))
                    .thenThrow(new OAuthAccessTokenNotFoundException("Access token not found"));

            boolean result = memberService.isAccessTokenExpired(testMemberInfo, accessToken);

            assertTrue(result); // 예외 발생 시 true 반환
            verify(oAuthService).isAccessTokenExpired(testMemberInfo.provider(), accessToken);
        }

        @Test
        @WithMockMember
        @DisplayName("Access Token 갱신 성공")
        void testRefreshAccessToken_Success() {
            MockHttpServletRequest request = new MockHttpServletRequest();

            // 쿠키에 refresh_token 설정
            Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
            request.setCookies(refreshTokenCookie);

            NaverToken naverNewToken = NaverToken.builder().accessToken("new-access-token").refreshToken("valid-refresh-token").tokenType("test-token-type").expiresIn(1000).result("success").error("test-error").errorDescription("test-error-description").build();

            when(securityUtil.getMemberInfo()).thenReturn(memberInfo);
            when(securityUtil.getOptionalRefreshToken(request)).thenReturn(refreshToken);
            when(oAuthService.refresh(any(), eq(refreshToken))).thenReturn(naverNewToken);
            doNothing().when(securityUtil).refreshAccessTokenInSecurityContext(any());

            String newAccessToken = memberService.refreshAccessToken(request);

            assertEquals("new-access-token", newAccessToken);
            verify(oAuthService).refresh(any(), eq(refreshToken));
            verify(securityUtil).refreshAccessTokenInSecurityContext("new-access-token");
        }

        @Test
        @DisplayName("Refresh Token 없음")
        void testRefreshAccessToken_WhenRefreshTokenNotFound_ShouldThrowUnauthorizedException() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            when(securityUtil.getOptionalRefreshToken(request)).thenReturn(null);

            assertThrows(UnauthorizedException.class, () -> {
                memberService.refreshAccessToken(request);
            });
            verify(securityUtil).clearContextAndDeleteCookie();
        }
    }
}