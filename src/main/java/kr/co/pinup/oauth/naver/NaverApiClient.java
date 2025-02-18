package kr.co.pinup.oauth.naver;

import jakarta.annotation.PostConstruct;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.oauth.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

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
    public NaverToken requestAccessToken(OAuthLoginParams params) {
        try {
            NaverToken tokenResponse = naverWebClient.get()
                    .uri(uriBuilder -> URI.create(UriComponentsBuilder.fromHttpUrl(naverProvider.getTokenUri())
                            .queryParam("grant_type", naverRegistration.getAuthorizationGrantType())
                            .queryParam("client_id", naverRegistration.getClientId())
                            .queryParam("client_secret", naverRegistration.getClientSecret())
                            .queryParam("state", params.makeParams().getFirst("state"))
                            .queryParam("code", params.makeParams().getFirst("code"))
                            .toUriString()))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("네이버에서 토큰을 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new OAuthTokenRequestException("네이버에서 액세스 토큰을 가져오지 못했습니다.");
            }
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuthTokenRequestException("Naver 액세스 토큰 요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public Pair<OAuthResponse, OAuthToken> requestOauth(OAuthToken token) {
        try {
            NaverResponse userDto = naverWebClient.get()
                    .uri(naverProvider.getUserInfoUri())
                    .header("Authorization", "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuth2AuthenticationException("네이버에서 사용자 정보를 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverResponse.class)
                    .block();

            if (userDto == null) {
                throw new OAuth2AuthenticationException("네이버에서 사용자 정보를 가져오지 못했습니다");
            }
            return Pair.of(userDto, token);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Naver 사용자 정보 요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public NaverToken refreshAccessToken(String refreshToken) {
        try {
            NaverToken tokenResponse = naverWebClient.get()
                    .uri(uriBuilder -> URI.create(UriComponentsBuilder.fromHttpUrl(naverProvider.getTokenUri())
                            .queryParam("grant_type", "refresh_token")
                            .queryParam("client_id", naverRegistration.getClientId())
                            .queryParam("client_secret", naverRegistration.getClientSecret())
                            .queryParam("refresh_token", refreshToken)
                            .toUriString()))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("네이버에서 토큰을 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new OAuthTokenRequestException("네이버에서 액세스 토큰을 다시 가져오지 못했습니다");
            }
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuthTokenRequestException("Naver 액세스 토큰 재요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public boolean revokeAccessToken(String accessToken) {
        try {
            NaverToken tokenResponse = naverWebClient.post()
                    .uri(uriBuilder -> URI.create(UriComponentsBuilder.fromHttpUrl(naverProvider.getTokenUri())
                            .queryParam("grant_type", "delete")
                            .queryParam("client_id", naverRegistration.getClientId())
                            .queryParam("client_secret", naverRegistration.getClientSecret())
                            .queryParam("accessToken", accessToken)
                            .queryParam("service_provider", OAuthProvider.NAVER)
                            .toUriString()))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("네이버에서 액세스 토큰을 취소하는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() != null) {
                throw new OAuthTokenRequestException("네이버에서 액세스 토큰을 취소하지 못했습니다");
            }
            return true;
        } catch (Exception e) {
            throw new OAuthTokenRequestException("Naver 액세스 토큰 취소 중 오류 발생: " + e.getMessage());
        }
    }
}