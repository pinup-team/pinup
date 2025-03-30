package kr.co.pinup.oauth.naver;

import jakarta.annotation.PostConstruct;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.members.exception.OAuth2AuthenticationException;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import kr.co.pinup.oauth.*;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class NaverApiClient implements OAuthApiClient {
    private final OauthConfig oauthConfig;
    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;
    private WebClient naverWebClient;
    private SecurityUtil securityUtil;

    @PostConstruct
    private void initOauthConfig() {
        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.naverProvider = oauthConfig.getProvider().get("naver");
        this.naverWebClient = WebClient.builder().build();
        this.securityUtil = new SecurityUtil();
    }

    @Override
    public OAuthProvider oAuthProvider() {
        return OAuthProvider.NAVER;
    }

    @Override
    public NaverToken requestAccessToken(OAuthLoginParams params) {
        try {
            URI tokenUri = URI.create(naverProvider.getTokenUri())
                    .resolve("?grant_type=" + naverRegistration.getAuthorizationGrantType()
                            + "&client_id=" + naverRegistration.getClientId()
                            + "&client_secret=" + naverRegistration.getClientSecret()
                            + "&state=" + params.makeParams().getFirst("state")
                            + "&code=" + params.makeParams().getFirst("code"));

            NaverToken tokenResponse = naverWebClient.get()
                    .uri(tokenUri)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("네이버에서 토큰을 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverToken.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                securityUtil.clearContextAndDeleteCookie();
                throw new OAuthTokenRequestException("네이버에서 액세스 토큰을 가져오지 못했습니다.");
            }
            return tokenResponse;
        } catch (Exception e) {
            securityUtil.clearContextAndDeleteCookie();
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
                securityUtil.clearContextAndDeleteCookie();
                throw new OAuth2AuthenticationException("네이버에서 사용자 정보를 가져오지 못했습니다");
            }
            return Pair.of(userDto, token);
        } catch (Exception e) {
            securityUtil.clearContextAndDeleteCookie();
            throw new OAuth2AuthenticationException("Naver 사용자 정보 요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public OAuthResponse isAccessTokenExpired(String accessToken) {
        try {
            return naverWebClient.get()
                    .uri(naverProvider.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            clientResponse -> {
                                throw new OAuthAccessTokenNotFoundException("네이버의 Access Token이 유효하지 않거나 만료되었습니다: " + clientResponse.statusCode());
                            })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new OAuth2AuthenticationException("네이버에서 사용자 검증을 진행하는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverResponse.class)
                    .block();
        } catch (OAuthAccessTokenNotFoundException e) {
            return null;
        }
    }

    @Override
    public NaverToken refreshAccessToken(String refreshToken) {
        try {
            URI tokenUri = URI.create(naverProvider.getTokenUri())
                    .resolve("?grant_type=refresh_token"
                            + "&client_id=" + naverRegistration.getClientId()
                            + "&client_secret=" + naverRegistration.getClientSecret()
                            + "&refresh_token=" + refreshToken);

            NaverToken tokenResponse = naverWebClient.post()
                    .uri(tokenUri) // URI 사용
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("네이버에서 토큰을 가져오는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverToken.class)
                    .block();

            if (tokenResponse == null || (tokenResponse.getError() != null && tokenResponse.getErrorDescription() != null)) {
                securityUtil.clearContextAndDeleteCookie();
                assert tokenResponse != null;
                throw new OAuthTokenRequestException("네이버에서 액세스 토큰을 다시 가져오지 못했습니다  : "+tokenResponse.getErrorDescription());
            }
            return tokenResponse;
        } catch (Exception e) {
            securityUtil.clearContextAndDeleteCookie();
            throw new OAuthTokenRequestException("Naver 액세스 토큰 재요청 중 오류 발생: " + e.getMessage());
        }
    }

    @Override
    public boolean revokeAccessToken(String accessToken) {
        try {
            URI revokeUri = URI.create(naverProvider.getTokenUri())
                    .resolve("?grant_type=delete"
                            + "&client_id=" + naverRegistration.getClientId()
                            + "&client_secret=" + naverRegistration.getClientSecret()
                            + "&access_token=" + accessToken
                            + "&service_provider=" + OAuthProvider.NAVER);

            NaverToken tokenResponse = naverWebClient.post()
                    .uri(revokeUri)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new OAuthTokenRequestException("네이버에서 액세스 토큰을 취소하는 중 오류가 발생했습니다: " + clientResponse.statusCode())))
                    .bodyToMono(NaverToken.class)
                    .block();

            return tokenResponse != null && "success".equals(tokenResponse.getResult());
        } catch (Exception e) {
            throw new OAuthTokenRequestException("Naver 액세스 토큰 취소 중 오류 발생: " + e.getMessage());
        }
    }
}