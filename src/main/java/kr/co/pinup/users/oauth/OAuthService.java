package kr.co.pinup.users.oauth;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
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
        OAuthApiClient client = clients.get(params.oAuthProvider());
        String accessToken = client.requestAccessToken(params);
        session.setAttribute("accessToken", accessToken);
        return client.requestOauth(accessToken);
    }

    public boolean revoke(OAuthProvider oAuthProvider, HttpSession session) {
        OAuthApiClient client = clients.get(oAuthProvider);
        String accessToken = (String) session.getAttribute("accessToken");
        return client.revokeAccessToken(session, accessToken);
    }
}
