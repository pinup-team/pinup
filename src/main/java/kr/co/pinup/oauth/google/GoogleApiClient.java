package kr.co.pinup.oauth.google;

import jakarta.annotation.PostConstruct;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.custom.utils.SecurityUtil;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.oauth.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleApiClient implements OAuthApiClient {
    private final OauthConfig oauthConfig;
    private OauthConfig.Registration googleRegistration;
    private OauthConfig.Provider googleProvider;
    private WebClient googleWebClient;
    private SecurityUtil securityUtil;

    @PostConstruct
    private void initOauthConfig() {
        this.googleRegistration = oauthConfig.getRegistration().get("google");
        this.googleProvider = oauthConfig.getProvider().get("google");
        this.googleWebClient = WebClient.builder()
                .defaultHeader("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
                .build();
        this.securityUtil = new SecurityUtil();
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
                                    .with("redirect_uri", googleRegistration.getRedirectUri())
                    )
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("구글에서 토큰을 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(GoogleToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                securityUtil.clearContextAndDeleteCookie();
                throw new OAuthTokenRequestException("구글에서 액세스 토큰을 가져오지 못했습니다.");
            }
            return tokenResponse;
        } catch (Exception e) {
            securityUtil.clearContextAndDeleteCookie();
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
                securityUtil.clearContextAndDeleteCookie();
                throw new OAuth2AuthenticationException("구글에서 사용자 정보를 가져오지 못했습니다");
            }
            return Pair.of(userDto, token);
        } catch (Exception e) {
            securityUtil.clearContextAndDeleteCookie();
            throw new OAuth2AuthenticationException("Google 사용자 정보 요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public OAuthResponse isAccessTokenExpired(String accessToken) {
        try {
            GoogleResponse userDto = googleWebClient.get()
                    .uri(googleProvider.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(),
                            clientResponse -> {
                                throw new OAuthAccessTokenNotFoundException("구글의 Access Token이 유효하지 않거나 만료되었습니다: " + clientResponse.statusCode());
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuth2AuthenticationException("구글에서 사용자 검증을 진행하는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(GoogleResponse.class)
                    .block();

            return userDto;
        } catch (OAuthAccessTokenNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public GoogleToken refreshAccessToken(String refreshToken) {
        try {
            GoogleToken tokenResponse = googleWebClient.post()
                    .uri(googleProvider.getTokenUri())
                    .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                            .with("client_id", googleRegistration.getClientId())
                            .with("client_secret", googleRegistration.getClientSecret())
                            .with("refresh_token", refreshToken)
                    )
                    .retrieve()
                    .bodyToMono(GoogleToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                securityUtil.clearContextAndDeleteCookie();
                throw new OAuthTokenRequestException("구글에서 액세스 토큰을 다시 가져오지 못했습니다.");
            }
            return tokenResponse;
        } catch (Exception e) {
            log.error("Google Access Token Refresh Fail : {}", e.getMessage());
            securityUtil.clearContextAndDeleteCookie();
            throw new OAuthTokenRequestException("Google 액세스 토큰 재요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public boolean revokeAccessToken(String accessToken) {
        try {
            var responseEntity = googleWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("oauth2.googleapis.com")
                            .path("/revoke")
                            .build()
                    )
                    .body(BodyInserters.fromFormData("token", accessToken))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Google Access Token Revoke Failed: {}, Response: {}", clientResponse.statusCode(), errorBody);
                                        return Mono.error(new OAuthTokenRequestException("Google에서 액세스 토큰을 취소하는 중 오류가 발생했습니다: " + clientResponse.statusCode()));
                                    }))
                    .toBodilessEntity()
                    .block();

            return responseEntity != null && responseEntity.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Google Access Token Revoke Fail : {}", e.getMessage());
            throw new OAuthTokenRequestException("Google 액세스 토큰 취소 중 오류 발생: " + e.getMessage());
        }
    }
}
