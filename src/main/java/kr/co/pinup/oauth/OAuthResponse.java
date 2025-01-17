package kr.co.pinup.oauth;

public interface OAuthResponse {
    String getId();
    String getName();
    String getEmail();
    OAuthProvider getOAuthProvider();
}
