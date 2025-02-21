package kr.co.pinup.oauth;

import org.apache.commons.lang3.tuple.Pair;

public interface OAuthApiClient {
    OAuthProvider oAuthProvider();
    OAuthToken requestAccessToken(OAuthLoginParams params);
    Pair<OAuthResponse, OAuthToken> requestOauth(OAuthToken token);
    OAuthResponse isAccessTokenExpired(String accessToken);
    OAuthToken refreshAccessToken(String refreshToken);
    boolean revokeAccessToken(String accessToken);
}
