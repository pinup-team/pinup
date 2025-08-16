package kr.co.pinup.oauth;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.members.exception.OAuthAccessTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthLoginCanceledException;
import kr.co.pinup.members.exception.OAuthProviderNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthService {
    private final Map<OAuthProvider, OAuthApiClient> clients;
    private final AppLogger appLogger;

    public OAuthService(List<OAuthApiClient> clients, AppLogger appLogger) {
        this.clients = clients.stream().collect(
                Collectors.toUnmodifiableMap(OAuthApiClient::oAuthProvider, Function.identity())
        );
        this.appLogger = appLogger;
    }

    public Pair<OAuthResponse, OAuthToken> request(OAuthLoginParams params) {
        String error = Optional.ofNullable(params.catchErrors().getFirst("error")).orElse("");

        if ("access_denied".equals(error)) {
            appLogger.warn(new WarnLog("OAuth 로그인 취소")
                    .addDetails("reason", "access_denied"));
            throw new OAuthLoginCanceledException();
        }

        OAuthApiClient client = Optional.ofNullable(clients.get(params.oAuthProvider()))
                .orElseThrow(() -> {
                    String msg = params.oAuthProvider() + "는 지원하지 않는 OAuth 제공자입니다.";
                    appLogger.warn(new WarnLog(msg));
                    return new OAuthProviderNotFoundException(msg);
                });

        OAuthToken token = Optional.ofNullable(client.requestAccessToken(params))
                .orElseThrow(() -> {
                    String msg = "OAuth 액세스 토큰 요청에 실패했습니다.";
                    appLogger.warn(new WarnLog(msg));
                    return new OAuthTokenRequestException(msg);
                });

        return client.requestUserInfo(token);
    }

    public OAuthResponse isAccessTokenExpired(OAuthProvider oAuthProvider, String accessToken) {
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> {
                    String msg = oAuthProvider + "는 지원하지 않는 OAuth 제공자입니다.";
                    appLogger.warn(new WarnLog(msg));
                    return new OAuthProviderNotFoundException(msg);
                });

        return Optional.ofNullable(client.isAccessTokenExpired(accessToken))
                .orElseThrow(() -> {
                    String msg = "액세스 토큰이 만료되었습니다.";
                    appLogger.warn(new WarnLog(msg));
                    return new OAuthAccessTokenNotFoundException(msg);
                });
    }

    public OAuthToken refresh(OAuthProvider oAuthProvider, String refreshToken) {
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> {
                    String msg = oAuthProvider + "는 지원하지 않는 OAuth 제공자입니다.";
                    appLogger.warn(new WarnLog(msg));
                    return new OAuthProviderNotFoundException(msg);
                });

        OAuthToken token = Optional.ofNullable(client.refreshAccessToken(refreshToken))
                .orElseThrow(() -> {
                    String msg = "액세스 토큰 요청에 실패했습니다.";
                    appLogger.warn(new WarnLog(msg));
                    return new OAuthTokenRequestException(msg);
                });

        if (token != null) appLogger.info(new InfoLog(oAuthProvider + " Access Token 재발급 성공"));

        return token;
    }

    public boolean revoke(OAuthProvider oAuthProvider, String accessToken) {
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> {
                    String msg = oAuthProvider + "는 지원하지 않는 OAuth 제공자입니다.";
                    appLogger.warn(new WarnLog(msg));
                    return new OAuthProviderNotFoundException(msg);
                });

        return client.revokeAccessToken(accessToken);
    }
}
