package kr.co.pinup.members.oauth;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.InputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.Charset.defaultCharset;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.util.StreamUtils.copyToString;

public class OAuthMocks {
    private final OauthConfig oauthConfig;
    private final OauthConfig.Registration naverRegistration;
    private final OauthConfig.Registration googleRegistration;
    private final OauthConfig.Provider naverProvider;
    private final OauthConfig.Provider googleProvider;
    HttpSession session;

    public OAuthMocks(OauthConfig oauthConfig) {
        this.oauthConfig = oauthConfig;
        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.googleRegistration = oauthConfig.getRegistration().get("google");
        this.naverProvider = oauthConfig.getProvider().get("naver");
        this.googleProvider = oauthConfig.getProvider().get("google");
    }

    public void setupResponse() throws IOException {
//        setupMockTokenResponse();
//        setupMockUserInformationResponse();

        setupNaverMockTokenResponse();
        setupGoogleMockTokenResponse();

        setupNaverMockUserInformationResponse();
        setupGoogleMockUserInformationResponse();
    }

    public void setupMockTokenResponse() throws IOException {
        stubFor(post(urlEqualTo("/?client_id=clientId&redirect_uri=redirectUri&code=code"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getMockResponseBodyByPath("payload/oauth-token-response.json"))
                )
        );
    }

    public void setupNaverMockTokenResponse() throws IOException {
        stubFor(post(urlEqualTo("/test/members/naver/token"))
//                wireMockServer.stubFor(post(urlEqualTo(naverProvider.getTokenUri()))
                .withQueryParam("client_id", matching(naverRegistration.getClientId()))
                .withQueryParam("client_secret", matching(naverRegistration.getClientSecret()))
                .withQueryParam("grant_type", matching(naverRegistration.getAuthorizationGrantType()))
                .withQueryParam("code", matching("oauthTestCode"))
                .withQueryParam("state", matching("oauthTestState"))
                .willReturn(aResponse().withStatus(200).withBody("{ \"access_token\": \"mock-access-token\", \"refresh_token\": \"mock-refresh-token\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\" }")));
    }

    public void setupGoogleMockTokenResponse() throws IOException {
        stubFor(post(urlEqualTo("/test/members/google/token"))
                //                wireMockServer.stubFor(post(urlEqualTo(googleProvider.getTokenUri()))
                .withQueryParam("client_id", matching(googleRegistration.getClientId()))
                .withQueryParam("client_secret", matching(googleRegistration.getClientSecret()))
                .withQueryParam("grant_type", matching(googleRegistration.getAuthorizationGrantType()))
                .withQueryParam("code", matching("oauthTestCode"))
                .withQueryParam("redirect_uri", matching(googleRegistration.getRedirectUri()))
                .willReturn(aResponse().withStatus(200).withBody("{ \"access_token\": \"mock-access-token\", \"refresh_token\": \"mock-refresh-token\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\", \"scope\": \"read\" }")));
    }

    public void setupMockUserInformationResponse() throws IOException {
        stubFor(get(urlEqualTo("/v2/user/me"))
                .withHeader("Authorization", equalTo("bearer accessToken"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(getMockResponseBodyByPath("payload/oauth-login-response.json"))
                )
        );
    }

    public void setupNaverMockUserInformationResponse() throws IOException {
        stubFor(post(urlEqualTo("/test/members/naver/userInfo"))
//                wireMockServer.stubFor(post(urlEqualTo(naverProvider.getUserInfoUri()))
                .withHeader("Authorization", matching("Bearer mock-access-token-oauthTestCode"))
                .willReturn(aResponse().withStatus(200).withBody("{ \"response\": { \"id\": \"123456789\", \"name\": \"test user\", \"email\": \"testuser@naver.com\" }}")));
    }

    public void setupGoogleMockUserInformationResponse() throws IOException {
        stubFor(post(urlEqualTo("/test/members/google/userInfo"))
//                wireMockServer.stubFor(post(urlEqualTo(googleProvider.getUserInfoUri()))
                .withHeader("Authorization", matching("Bearer mock-access-token-oauthTestCode"))
                .willReturn(aResponse().withStatus(200).withBody("{ \"id\": \"a1b2c3d4e5_f6g7h8\", \"name\": \"test user\", \"email\": \"testuser@google.com\" }")));
    }

    private String getMockResponseBodyByPath(String path) throws IOException {
        return copyToString(getMockResourceStream(path), defaultCharset());
    }

    private InputStream getMockResourceStream(String path) {
        return OAuthMocks.class.getClassLoader().getResourceAsStream(path);
    }
}
