package kr.co.pinup.oauth;

public interface OAuthApiClient {
    OAuthProvider oAuthProvider();
    String requestAccessToken(OAuthLoginParams params);
    OAuthResponse requestOauth(String accessToken);
    boolean revokeAccessToken(String accessToken);
}
