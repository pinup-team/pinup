package kr.co.pinup.oauth;

import jakarta.servlet.http.HttpSession;

public interface OAuthApiClient {
    OAuthProvider oAuthProvider();
    String requestAccessToken(OAuthLoginParams params);
    OAuthResponse requestOauth(String accessToken);
    boolean revokeAccessToken(HttpSession session, String accessToken);
}
