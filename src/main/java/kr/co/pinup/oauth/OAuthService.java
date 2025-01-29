package kr.co.pinup.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.members.exception.OAuthProviderNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenNotFoundException;
import kr.co.pinup.members.exception.OAuthTokenRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthService {
    private final Map<OAuthProvider, OAuthApiClient> clients;

    public OAuthService(List<OAuthApiClient> clients) {
        this.clients = clients.stream().collect(
                Collectors.toUnmodifiableMap(OAuthApiClient::oAuthProvider, Function.identity())
        );
    }

    public OAuthResponse request(OAuthLoginParams params, HttpSession session) {
        OAuthApiClient client = Optional.ofNullable(clients.get(params.oAuthProvider()))
                .orElseThrow(() -> new OAuthProviderNotFoundException("지원하지 않는 OAuth 제공자입니다."));

        String accessToken = Optional.ofNullable(client.requestAccessToken(params))
                .orElseThrow(() -> new OAuthTokenRequestException("엑세스 토큰 요청에 실패했습니다."));

        session.setAttribute("accessToken", accessToken);
        return client.requestOauth(accessToken);
    }

    public boolean revoke(OAuthProvider oAuthProvider, HttpServletRequest request) {
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> new OAuthProviderNotFoundException("지원하지 않는 OAuth 제공자입니다."));

        String accessToken = Optional.ofNullable(request.getHeader("Authorization"))
                .orElseThrow(() -> new OAuthTokenNotFoundException("엑세스 토큰이 존재하지 않습니다."));

        return client.revokeAccessToken(accessToken);
    }
}
