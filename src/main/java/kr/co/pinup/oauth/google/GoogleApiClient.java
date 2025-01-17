package kr.co.pinup.oauth.google;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.oauth.OAuthApiClient;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class GoogleApiClient implements OAuthApiClient {
    private final OauthConfig oauthConfig;
    private OauthConfig.Registration googleRegistration;
    private OauthConfig.Provider googleProvider;
    private WebClient googleWebClient;

    @PostConstruct
    private void initOauthConfig() {
        this.googleRegistration = oauthConfig.getRegistration().get("google");
        this.googleProvider = oauthConfig.getProvider().get("google");
        this.googleWebClient = WebClient.builder()
                .defaultHeader("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
                .build();
    }

    @Override
    public OAuthProvider oAuthProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String requestAccessToken(OAuthLoginParams params) {
        GoogleToken tokenResponse = googleWebClient.post()
                .uri(googleProvider.getTokenUri())
                .body(BodyInserters.fromFormData("grant_type", googleRegistration.getAuthorizationGrantType())
                        .with("client_id", googleRegistration.getClientId())
                        .with("client_secret", googleRegistration.getClientSecret())
                        .with("code", params.makeParams().getFirst("code"))
                        .with("redirect_uri", googleRegistration.getRedirectUri()))
                .retrieve()
                .bodyToMono(GoogleToken.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new IllegalStateException("Failed to retrieve access token from Google");
        }
        return tokenResponse.getAccessToken();
    }

    @Override
    public GoogleResponse requestOauth(String accessToken) {
        GoogleResponse userDto = googleWebClient.get()
                .uri(googleProvider.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().bodyToMono(GoogleResponse.class).block();
        System.out.println("userDto" + userDto);
        return userDto;
    }

    @Override
    public boolean revokeAccessToken(HttpSession session, String accessToken) {
        return googleWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("oauth2.googleapis.com")
                        .path("/revoke")
                        .queryParam("token", accessToken)
                        .build()
                )
                .retrieve()
                .toBodilessEntity()
                .map(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful())
                .block();
    }
}
