package kr.co.pinup.oauth.google;

import jakarta.annotation.PostConstruct;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.oauth.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

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
    public GoogleToken requestAccessToken(OAuthLoginParams params) {
        try {
            GoogleToken tokenResponse = googleWebClient.post()
                    .uri(googleProvider.getTokenUri())
                    .body(BodyInserters.fromFormData("grant_type", googleRegistration.getAuthorizationGrantType())
                            .with("client_id", googleRegistration.getClientId())
                            .with("client_secret", googleRegistration.getClientSecret())
                            .with("code", Objects.requireNonNull(params.makeParams().getFirst("code")))
                            .with("redirect_uri", googleRegistration.getRedirectUri()))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("구글에서 토큰을 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(GoogleToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new OAuthTokenRequestException("구글에서 액세스 토큰을 가져오지 못했습니다.");
            }
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuthTokenRequestException("Google 액세스 토큰 요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public Pair<OAuthResponse, OAuthToken> requestOauth(OAuthToken token) {
        try {
            GoogleResponse userDto = googleWebClient.get()
                    .uri(googleProvider.getUserInfoUri())
                    .header("Authorization", "Bearer " + token.getAccessToken())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuth2AuthenticationException("구글에서 사용자 정보를 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(GoogleResponse.class).
                    block();

            if (userDto == null) {
                throw new OAuth2AuthenticationException("구글에서 사용자 정보를 가져오지 못했습니다");
            }
            return Pair.of(userDto, token);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Google 사용자 정보 요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public GoogleToken refreshAccessToken(String refreshToken) {
        try {
            GoogleToken tokenResponse = googleWebClient.post()
                    .uri(googleProvider.getTokenUri())
                    .body(BodyInserters.fromFormData("grant_type", googleRegistration.getAuthorizationGrantType())
                            .with("client_id", googleRegistration.getClientId())
                            .with("client_secret", googleRegistration.getClientSecret())
                            .with("grant_type", "refresh_token")
                            .with("redirect_uri", refreshToken))
                    .retrieve()
                    .bodyToMono(GoogleToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new OAuthTokenRequestException("구글에서 액세스 토큰을 다시 가져오지 못했습니다.");
            }
            return tokenResponse;
        } catch (Exception e) {
            throw new OAuthTokenRequestException("Google 액세스 토큰 재요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public boolean revokeAccessToken(String accessToken) {
        if (googleWebClient == null) {
            throw new IllegalStateException("googleWebClient is not initialized");
        }
        try {
            // 결과를 block()으로 직접 받아서 처리
            var responseEntity = googleWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("oauth2.googleapis.com")
                            .path("/revoke")
                            .queryParam("token", accessToken)
                            .build()
                    )
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("Google에서 액세스 토큰을 취소하는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .toBodilessEntity() // 응답을 body-less 엔티티로 받음
                    .block(); // block()을 사용해 결과를 기다림

            // 결과가 2xx 응답일 경우 true, 4xx 응답일 경우 false 반환
            if (responseEntity != null) {
                return responseEntity.getStatusCode().is2xxSuccessful();
            }
            return false; // 응답이 null이거나 실패한 경우 false 반환
        } catch (Exception e) {
            throw new OAuthTokenRequestException("Google 액세스 토큰 취소 중 오류 발생: " + e.getMessage());
        }
//        return Boolean.TRUE.equals(googleWebClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .scheme("https")
//                        .host("oauth2.googleapis.com")
//                        .path("/revoke")
//                        .queryParam("token", accessToken)
//                        .build()
//                )
//                .retrieve()
//                .toBodilessEntity()
//                .map(responseEntity -> responseEntity.getStatusCode().is2xxSuccessful())
//                .block());
    }
}
