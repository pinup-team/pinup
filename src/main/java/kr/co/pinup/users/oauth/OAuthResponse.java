package kr.co.pinup.users.oauth;

public interface OAuthResponse {
    String getId();
    String getName();
    String getEmail();
    OAuthProvider getOAuthProvider();
}
