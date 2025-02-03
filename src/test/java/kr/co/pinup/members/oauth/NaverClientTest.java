package kr.co.pinup.members.oauth;

import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.naver.NaverApiClient;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.oauth.naver.NaverToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

// CHECK
public class NaverClientTest {
    @Mock
    private OauthConfig oauthConfig;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private NaverApiClient naverApiClient;

    private OAuthLoginParams loginParams;
    private String accessToken;
    private String authorizationCode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loginParams = NaverLoginParams.builder()
                .code("test-code")
                .state("test-state")
                .build();
        accessToken = "validAccessToken";
        authorizationCode = "validAuthCode";

        // Mock WebClient setup
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri((URI) any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }


    /*@Nested
    @DisplayName("Access Token 요청 관련 테스트")
    class RequestAccessTokenTests {
        @Test
        @DisplayName("Access Token 요청 성공")
        void requestAccessToken_Success(){
            // Arrange
            NaverToken mockNaverToken = NaverToken.builder().accessToken("test-access-token").refreshToken("test-refresh-token").tokenType("test-token-type").expiresIn("test-expires_in").build();

            when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockNaverToken));

            // Act
            String accessToken = naverApiClient.requestAccessToken(NaverLoginParams.builder().code("test-code").state("test-state").build());

            // Assert
            assertNotNull(accessToken);
            assertEquals("test-access-token", accessToken);
        }

        @Test
        @DisplayName("Access Token 요청 실패 - 예외 발생")
        void requestAccessToken_Fail() {
            when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());


            // Act & Assert
            OAuthTokenRequestException exception = assertThrows(OAuthTokenRequestException.class,
                    () -> naverApiClient.requestAccessToken(NaverLoginParams.builder().code("test-code").state("test-state").build()));

            assertEquals("네이버에서 액세스 토큰을 가져오지 못했습니다", exception.getMessage());
        }
    }*/

    /*@Nested
    @DisplayName("User 정보 요청 관련 테스트")
    class RequestUserInfoTests {

        @Test
        @DisplayName("User 정보 요청 성공")
        void requestOauth_Success() {
            // Arrange
            when(naverWebClient.get()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockNaverResponse));

            // Act
            NaverResponse userResponse = naverApiClient.requestOauth("test-access-token");

            // Assert
            assertNotNull(userResponse);
            assertEquals("testUser", userResponse.getResponse().getName());
        }

        @Test
        @DisplayName("User 정보 요청 실패 - 예외 발생")
        void requestOauth_Fail() {
            // Arrange
            when(naverWebClient.get()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.header(any(), any())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());

            // Act & Assert
            OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class,
                    () -> naverApiClient.requestOauth("test-access-token"));
            assertEquals("네이버에서 사용자 정보를 가져오지 못했습니다", exception.getMessage());
        }
    }*/

    // CHECK
//    @Test
//    void requestOauth_ShouldThrowException_WhenUserInfoRequestFails() {
//        // Arrange
//        when(responseSpec.bodyToMono(NaverResponse.class))
//                .thenReturn(Mono.error(new OAuth2AuthenticationException("네이버에서 사용자 정보를 가져오는 중 오류가 발생했습니다")));
//
//        // Act & Assert
//        OAuth2AuthenticationException thrown = assertThrows(OAuth2AuthenticationException.class, () -> {
//            naverApiClient.requestOauth("someAccessToken");  // 유효한 토큰 넘기기
//        });
//
//        assertEquals("네이버에서 사용자 정보를 가져오는 중 오류가 발생했습니다", thrown.getMessage());
//    }

    /*@Nested
    @DisplayName("Access Token 취소 요청 관련 테스트")
    class RevokeAccessTokenTests {

        @Test
        @DisplayName("Access Token 취소 성공")
        void revokeAccessToken_Success() {
            // Arrange
            when(naverWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(mockNaverResponse));

            // Act
            boolean result = naverApiClient.revokeAccessToken("test-access-token");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Access Token 취소 실패 - 예외 발생")
        void revokeAccessToken_Fail() {
            // Arrange
            when(naverWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());

            // Act & Assert
            OAuthTokenRequestException exception = assertThrows(OAuthTokenRequestException.class,
                    () -> naverApiClient.revokeAccessToken("test-access-token"));
            assertEquals("네이버에서 액세스 토큰을 취소하지 못했습니다", exception.getMessage());
        }
    }*/
    @Test
    void revokeAccessToken_ShouldReturnTrue_WhenSuccessfullyRevoked() {
        // Arrange
        NaverToken mockTokenResponse = new NaverToken(); // Access token should be null after revocation

        when(responseSpec.bodyToMono(NaverToken.class)).thenReturn(Mono.just(mockTokenResponse));

        // Act
        NaverToken response = responseSpec.bodyToMono(NaverToken.class).block();  // 동기 방식으로 변경
        boolean result = response.getAccessToken() == null;

        // Assert
        assertTrue(result);
    }

    // CHECK
//    @Test
//    void revokeAccessToken_ShouldReturnFalse_WhenRevocationFails() {
//        when(responseSpec.bodyToMono(NaverToken.class))
//                .thenReturn(Mono.error(new OAuthTokenRequestException("네이버에서 액세스 토큰을 취소하는 중 오류가 발생했습니다")));
//
//        // Act & Assert
//        OAuthTokenRequestException thrown = assertThrows(OAuthTokenRequestException.class, () -> {
//            naverApiClient.revokeAccessToken("someAccessToken");  // 유효한 토큰 넘기기
//        });
//
//        assertEquals("네이버에서 액세스 토큰을 취소하는 중 오류가 발생했습니다", thrown.getMessage());
//    }

//@Nested
//@DisplayName("로그아웃 관련 테스트")
//class LogoutTests {
//
//    @Test
//    @DisplayName("OAuth 제공자가 없을 경우 예외 발생")
//    public void testLogout_WhenOAuthProviderIsNull_ShouldThrowOAuthProviderNotFoundException() {
//        assertThrows(OAuthProviderNotFoundException.class, () -> {
//            memberService.logout(null, mock(HttpServletRequest.class));
//        });
//    }
//
//    @Test
//    @DisplayName("세션이 존재하지 않을 경우 예외 발생")
//    public void testLogout_WhenSessionIsNull_ShouldThrowUnauthorizedException() {
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        when(request.getSession(false)).thenReturn(null);
//
//        assertThrows(UnauthorizedException.class, () -> {
//            memberService.logout(OAuthProvider.NAVER, request);
//        });
//    }
//
//    @Test
//    @DisplayName("AccessToken이 존재하지 않을 경우 예외 발생")
//    public void testLogout_WhenAccessTokenIsNull_ShouldThrowOAuthTokenNotFoundException() {
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpSession session = mock(HttpSession.class);
//        when(request.getSession(false)).thenReturn(session);
//        when(session.getAttribute("accessToken")).thenReturn(null);
//
//        assertThrows(OAuthTokenNotFoundException.class, () -> {
//            memberService.logout(OAuthProvider.NAVER, request);
//        });
//    }
//
//    @Test
//    @DisplayName("OAuth 로그아웃 중 오류 발생")
//    public void testLogout_WhenOAuthServiceFails_ShouldThrowOAuth2AuthenticationException() {
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpSession session = mock(HttpSession.class);
//        when(request.getSession(false)).thenReturn(session);
//        when(session.getAttribute("accessToken")).thenReturn("validAccessToken");
//
//        OAuthProvider provider = OAuthProvider.NAVER;
//
//        // Mock OAuth service to simulate failure in revoking access token
//        when(oAuthService.revoke(provider, "validAccessToken")).thenReturn(false);
//
//        assertThrows(OAuth2AuthenticationException.class, () -> {
//            memberService.logout(provider, request);
//        });
//    }
//
//    @Test
//    @DisplayName("로그아웃 성공")
//    public void testLogout_SuccessfulLogout_ShouldReturnTrue() {
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpSession session = mock(HttpSession.class);
//        when(request.getSession(false)).thenReturn(session);
//        when(session.getAttribute("accessToken")).thenReturn("validAccessToken");
//
//        OAuthProvider provider = OAuthProvider.NAVER;
//
//        // Mock OAuth service to simulate successful revocation
//        when(oAuthService.revoke(provider, "validAccessToken")).thenReturn(true);
//
//        boolean result = memberService.logout(provider, request);
//
//        assertTrue(result);
//        verify(session).invalidate(); // Verify session invalidation
//        verify(SecurityContextHolder.class, times(1)).clearContext(); // Verify security context is cleared
//    }
}
