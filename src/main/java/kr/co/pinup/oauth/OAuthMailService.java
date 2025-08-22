package kr.co.pinup.oauth;

public interface OAuthMailService {
    void sendVerificationCode(String toEmail, String code);
}