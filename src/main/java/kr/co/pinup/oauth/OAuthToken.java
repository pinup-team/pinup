package kr.co.pinup.oauth;

public interface OAuthToken {
    String getAccessToken();
    String getRefreshToken();
}