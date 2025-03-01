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
import kr.co.pinup.oauth.google.GoogleResponse;
import kr.co.pinup.oauth.google.GoogleToken;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class GoogleOAuthTest {
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
    private GoogleResponse googleResponse;
    private GoogleToken googleToken;
    private Pair<OAuthResponse, OAuthToken> googlePair;

    private GoogleLoginParams params;

    private String accessToken = "valid-access-token";
    private String refreshToken = "valid-refresh-token";

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .name("test")
                .email("test@google.com")
                .nickname("구글TestMember")
                .providerType(OAuthProvider.GOOGLE)
                .providerId("testId123456789")
                .role(MemberRole.ROLE_USER)
                .build();
        memberInfo = MemberInfo.builder()
                .nickname("구글TestMember")
                .provider(OAuthProvider.GOOGLE)
                .role(MemberRole.ROLE_USER)
                .build();

        params = GoogleLoginParams.builder().code("test-code").state("test-state").build();

        googleResponse = GoogleResponse.builder().sub("testId123456789").name("test").email("test@google.com").build();
        googleToken = GoogleToken.builder().accessToken("valid-access-token").refreshToken("valid-refresh-token").tokenType("Bearer").expiresIn(3920).scope("test-scope").build();
        googlePair = Pair.of(googleResponse, googleToken);

        // Mock 객체의 동작 정의
        doNothing().when(securityUtil).setAuthentication(googleToken, memberInfo);
    }

    @Nested
    @DisplayName("Google 로그인/회원가입 관련 테스트")
    class LoginMemberTests {
        private MockHttpSession session = new MockHttpSession();

        @Test
        @WithMockMember(nickname = "구글TestMember", provider = OAuthProvider.GOOGLE, role = MemberRole.ROLE_USER)
        @DisplayName("OAuth 로그인 성공")
        void login_Success() {
            when(oAuthService.request(any())).thenReturn(googlePair);
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

            // ✅ SecurityContext 검증
            SecurityContext securityContext = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertNotNull(securityContext);
            assertNotNull(securityContext.getAuthentication());
            assertEquals(memberInfo, securityContext.getAuthentication().getPrincipal());
        }

        @Test
        @DisplayName("로그인 실패_회원 정보 없음_회원가입 발생")
        void testLogin_WhenMemberNotFound_ShouldCreateNewMember() {
            when(oAuthService.request(any())).thenReturn(googlePair);
            when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty()); // 회원 정보가 없을 때
            when(memberRepository.save(any(Member.class))).thenReturn(member); // 새로운 회원 저장

            Pair<OAuthResponse, OAuthToken> result = memberService.login(GoogleLoginParams.builder()
                    .code("test-code")
                    .state("test-state")
                    .build(), session);
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
        @DisplayName("로그인 실패_OAuth 요청 실패")
        void testLogin_WhenOAuthRequestFails_ShouldThrowUnauthorizedException() {
            when(oAuthService.request(any())).thenThrow(new UnauthorizedException("Invalid OAuth request"));

            assertThrows(UnauthorizedException.class, () -> {
                memberService.login(GoogleLoginParams.builder()
                        .code("test-code")
                        .state("test-state")
                        .build(), session);
            });
        }
    }

    @Nested
    @DisplayName("Google 로그아웃 관련 테스트")
    class LogOutMemberTests {
        private MockHttpServletRequest request = new MockHttpServletRequest();
        private OAuthProvider oAuthProvider = OAuthProvider.GOOGLE;

        @Test
        @DisplayName("로그아웃 성공")
        void testLogout_Success() {
            when(securityUtil.getMemberInfo()).thenReturn(memberInfo);
            when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn(accessToken);
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
        @WithMockMember(nickname = "구글TestMember", provider = OAuthProvider.GOOGLE, role = MemberRole.ROLE_USER)
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

        @Test
        @DisplayName("Access Token 만료 확인 - 만료된 경우")
        void testIsAccessTokenExpired_WhenTokenIsExpired() {
            MemberInfo testMemberInfo = MemberInfo.builder()
                    .nickname("구글TestMember")
                    .provider(OAuthProvider.GOOGLE)
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
            MemberInfo testMemberInfo = MemberInfo.builder()
                    .nickname("구글TestMember")
                    .provider(OAuthProvider.GOOGLE)
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(oAuthService.isAccessTokenExpired(testMemberInfo.provider(), accessToken))
                    .thenReturn(googleResponse);

            when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(member));

            boolean result = memberService.isAccessTokenExpired(testMemberInfo, accessToken);

            assertFalse(result); // 만료되지 않은 경우 false 반환
            verify(oAuthService).isAccessTokenExpired(testMemberInfo.provider(), accessToken);
            verify(memberRepository).findByEmail(anyString());
        }

        @Test
        @DisplayName("Access Token 만료 확인 - 예외 발생 시")
        void testIsAccessTokenExpired_WhenExceptionOccurs() {
            MemberInfo testMemberInfo = MemberInfo.builder()
                    .nickname("구글TestMember")
                    .provider(OAuthProvider.GOOGLE)
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(oAuthService.isAccessTokenExpired(testMemberInfo.provider(), accessToken))
                    .thenThrow(new OAuthAccessTokenNotFoundException("Access token not found"));

            boolean result = memberService.isAccessTokenExpired(testMemberInfo, accessToken);

            assertTrue(result); // 예외 발생 시 true 반환
            verify(oAuthService).isAccessTokenExpired(testMemberInfo.provider(), accessToken);
        }

        @Test
        @WithMockMember(nickname = "구글TestMember", provider = OAuthProvider.GOOGLE, role = MemberRole.ROLE_USER)
        @DisplayName("Access Token 갱신 성공")
        void testRefreshAccessToken_Success() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            // 쿠키에 refresh_token 설정
            Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
            request.setCookies(refreshTokenCookie);

            GoogleToken googleNewToken = GoogleToken.builder().accessToken("new-access-token").refreshToken("valid-refresh-token").tokenType("Bearer").expiresIn(3920).scope("test-scope").build();

            when(securityUtil.getMemberInfo()).thenReturn(memberInfo);
            when(securityUtil.getOptionalRefreshToken(request)).thenReturn(refreshToken);
            when(oAuthService.refresh(any(), eq(refreshToken))).thenReturn(googleNewToken);
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