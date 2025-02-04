package kr.co.pinup.oauth;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.members.exception.OAuthProviderNotFoundException;
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
                .orElseThrow(() -> new OAuthProviderNotFoundException(params.oAuthProvider().toString()+"는 지원하지 않는 OAuth 제공자입니다."));

        String accessToken = Optional.ofNullable(client.requestAccessToken(params))
                .orElseThrow(() -> new OAuthTokenRequestException("엑세스 토큰 요청에 실패했습니다."));

        session.setAttribute("accessToken", accessToken);
        return client.requestOauth(accessToken);
    }

    public boolean revoke(OAuthProvider oAuthProvider, String accessToken) {
        OAuthApiClient client = Optional.ofNullable(clients.get(oAuthProvider))
                .orElseThrow(() -> new OAuthProviderNotFoundException(clients.get(oAuthProvider)+"는 지원하지 않는 OAuth 제공자입니다."));

        // CHECK header에 accessToken 제대로 들어감! test 때문에 일단은 session에 두기!
//        String accessToken = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
//                .filter(cookie -> "Authorization".equals(cookie.getName()))
//                .map(Cookie::getValue)
//                .findFirst()
//                .orElseThrow(() -> new OAuthTokenNotFoundException("엑세스 토큰이 존재하지 않습니다."));
        return client.revokeAccessToken(accessToken);
    }
}
