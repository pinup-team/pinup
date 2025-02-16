package kr.co.pinup.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpHeaders;

public interface OAuthApiClient {
    OAuthProvider oAuthProvider();
    OAuthToken requestAccessToken(OAuthLoginParams params);
    Pair<OAuthResponse, OAuthToken> requestOauth(OAuthToken token);
    OAuthToken refreshAccessToken(String refreshToken);
    boolean revokeAccessToken(String accessToken);
}
