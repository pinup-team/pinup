package kr.co.pinup.members.oauth;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.oauth.google.GoogleResponse;
import kr.co.pinup.oauth.google.GoogleToken;
import kr.co.pinup.oauth.naver.NaverResponse;
import kr.co.pinup.oauth.naver.NaverToken;
import org.springframework.boot.autoconfigure.codec.CodecProperties;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/api/members")
public class TestApiController {
    private final OauthConfig oauthConfig;
    private final CodecProperties codecProperties;
    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Registration googleRegistration;
    HttpSession session;

    public TestApiController(OauthConfig oauthConfig, CodecProperties codecProperties) {
        this.oauthConfig = oauthConfig;
        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.googleRegistration = oauthConfig.getRegistration().get("google");
        this.codecProperties = codecProperties;
    }

    @GetMapping("/naver/token")
    public NaverToken getNaverToken(@RequestParam("client_id") String clientId, @RequestParam("client_secret") String clientSecret,
                                    @RequestParam("grant_type") String grantType, @RequestParam("code") String code, @RequestParam String state) {
        System.out.println("getNaverToken");
        if(clientId.equals(naverRegistration.getClientId()) && clientSecret.equals(naverRegistration.getClientSecret())
                && grantType.equals(naverRegistration.getAuthorizationGrantType()) && code.equals("oauthTestCode") && state.equals("oauthTestState")) {
            NaverToken mockToken = NaverToken.builder()
                    .accessToken("mock-access-token-" + code)
                    .refreshToken("mock-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .build();

            return mockToken;
        } else {
            return null;
        }
    }

    @PostMapping("/naver/userInfo")
    public NaverResponse getNaverUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader.replace("Bearer ", "");

        String sessionToken = (String) session.getAttribute("accessToken");

        if(sessionToken.equals(accessToken)) {
            NaverResponse.Response response = new NaverResponse.Response(
                    "123456789", "테스트 네이버 사용자", "testuser@naver.com"
            );

            NaverResponse mockNaverResponse = NaverResponse.builder().response(NaverResponse.Response.builder().id("testId").name("testUser").email("test@naver.com").build()).build();
            mockNaverResponse.response = response;

            return mockNaverResponse;
        } else {
            return null;
        }
    }

    @PostMapping("/naver/token")
    public NaverToken revokeNaverToken(@RequestParam("client_id") String clientId, @RequestParam("client_secret") String clientSecret,
                                       @RequestParam("grant_type") String grantType, @RequestParam("code") String code, @RequestParam String state) {
        System.out.println("revokeNaverToken");
        if(clientId.equals(naverRegistration.getClientId()) && clientSecret.equals(naverRegistration.getClientSecret())
                && grantType.equals(naverRegistration.getAuthorizationGrantType()) && code.equals("oauthTestCode") && state.equals("oauthTestState")) {
            NaverToken mockToken = NaverToken.builder()
                    .accessToken("mock-access-token-" + code)
                    .refreshToken("mock-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .build();

            return mockToken;
        } else {
            return null;
        }
    }

    @GetMapping("/google/token")
    public GoogleToken getGoogleToken(@RequestParam("client_id") String clientId, @RequestParam("client_secret") String clientSecret,
                                      @RequestParam("grant_type") String grantType, @RequestParam("code") String code, @RequestParam("redirect_uri") String redirectUri) {
        System.out.println("getGoogleToken");
        if(clientId.equals(naverRegistration.getClientId()) && clientSecret.equals(naverRegistration.getClientSecret())
                && grantType.equals(naverRegistration.getAuthorizationGrantType()) && code.equals("oauthTestCode") && redirectUri.equals(googleRegistration.getRedirectUri())) {
            GoogleToken mockToken = GoogleToken.builder()
                    .accessToken("mock-access-token-" + code)
                    .refreshToken("mock-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .scope("read")
                    .build();

            return mockToken;
        } else {
            return null;
        }
    }

    @PostMapping("/google/userInfo")
    public GoogleResponse getGoogleUserInfo(@RequestHeader("Authorization") String authorizationHeader) {
        String accessToken = authorizationHeader.replace("Bearer ", "");

        String sessionToken = (String) session.getAttribute("accessToken");

        if (sessionToken.equals(accessToken)) {
            GoogleResponse mockGoogleResponse = new GoogleResponse(
                    "a1b2c3d4e5_f6g7h8", "테스트 구글 사용자", "testuser@google.com"
            );

            return mockGoogleResponse;
        } else {
            return null;
        }
    }

}
