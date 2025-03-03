package kr.co.pinup.oauth;

import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthProviderNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OAuthService {
    private final Map<OAuthProvider, OAuthApiClient> clients;

    public OAuthService(List<OAuthApiClient> clients) {
        this.clients = clients.stream().collect(
                Collectors.toUnmodifiableMap(OAuthApiClient::oAuthProvider, Function.identity())
        );
    }

    public Pair<OAuthResponse, OAuthToken> request(OAuthLoginParams params) {
        OAuthApiClient client = Optional.ofNullable(clients.get(params.oAuthProvider()))
                .orElseThrow(() -> new OAuthProviderNotFoundException(params.oAuthProvider().toString() + "는 지원하지 않는 OAuth 제공자입니다."));

        OAuthToken token = Optional.ofNullable(client.requestAccessToken(params))
                .orElseThrow(() -> new OAuthTokenRequestException("엑세스 토큰 요청에 실패했습니다."));

        return client.requestOauth(token);
    }

    public OAuthResponse isAccessTokenExpired(OAuthProvider oAuthProvider, String accessToken) {
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> new OAuthProviderNotFoundException(oAuthProvider.toString() + "는 지원하지 않는 OAuth 제공자입니다."));
        return Optional.ofNullable(client.isAccessTokenExpired(accessToken))
                .orElseThrow(() -> new OAuthAccessTokenNotFoundException("엑세스 토큰이 만료되었습니다."));
    }

    public OAuthToken refresh(OAuthProvider oAuthProvider, String refreshToken) {
        log.info("OAuthService refresh with OauthProvider {}", oAuthProvider);
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> new OAuthProviderNotFoundException(oAuthProvider + "는 지원하지 않는 OAuth 제공자입니다."));

        OAuthToken token = Optional.ofNullable(client.refreshAccessToken(refreshToken))
                .orElseThrow(() -> new OAuthTokenRequestException("엑세스 토큰 요청에 실패했습니다."));

        if (token != null) log.debug("OAuthService : {} Access Token Refresh Success", oAuthProvider);

        return token;
    }

    public boolean revoke(OAuthProvider oAuthProvider, String accessToken) {
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> new OAuthProviderNotFoundException(clients.get(oAuthProvider) + "는 지원하지 않는 OAuth 제공자입니다."));
        return client.revokeAccessToken(accessToken);
    }
}
