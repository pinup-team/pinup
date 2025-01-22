package kr.co.pinup.oauth.naver;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.members.config.OauthConfig;
import kr.co.pinup.oauth.OAuthApiClient;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class NaverApiClient implements OAuthApiClient {
    private final OauthConfig oauthConfig;
    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;
    private WebClient naverWebClient;

    @PostConstruct
    private void initOauthConfig() {
        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.naverProvider = oauthConfig.getProvider().get("naver");
        this.naverWebClient = WebClient.builder().build();
    }

    @Override
    public OAuthProvider oAuthProvider() {
        return OAuthProvider.NAVER;
    }

    @Override
    public String requestAccessToken(OAuthLoginParams params) {
        NaverToken tokenResponse = naverWebClient.get()
                .uri(uriBuilder -> URI.create(UriComponentsBuilder.fromHttpUrl(naverProvider.getTokenUri())
                        .queryParam("grant_type", naverRegistration.getAuthorizationGrantType())
                        .queryParam("client_id", naverRegistration.getClientId())
                        .queryParam("client_secret", naverRegistration.getClientSecret())
                        .queryParam("state", params.makeParams().getFirst("state"))
                        .queryParam("code", params.makeParams().getFirst("code"))
                        .toUriString()))
                .retrieve()
                .bodyToMono(NaverToken.class)
                .block();

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new IllegalStateException("Failed to retrieve access token from Naver");
        }
        return tokenResponse.getAccessToken();
    }

    @Override
    public NaverResponse requestOauth(String accessToken) {
        NaverResponse userDto = naverWebClient.get()
                .uri(naverProvider.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().bodyToMono(NaverResponse.class).block();
        System.out.println("userDto" + userDto);
        return userDto;
    }

    @Override
    public boolean revokeAccessToken(HttpSession session, String accessToken) {
        NaverToken tokenResponse = naverWebClient.post()
                .uri(uriBuilder -> URI.create(UriComponentsBuilder.fromHttpUrl(naverProvider.getTokenUri())
                        .queryParam("grant_type", "delete")
                        .queryParam("client_id", naverRegistration.getClientId())
                        .queryParam("client_secret", naverRegistration.getClientSecret())
                        .queryParam("accessToken", accessToken)
                        .queryParam("service_provider", OAuthProvider.NAVER)
                        .toUriString()))
                .retrieve()
                .bodyToMono(NaverToken.class)
                .block();
        assert tokenResponse != null;
        return tokenResponse.getAccessToken() == null;
    }
}
