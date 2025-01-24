package kr.co.pinup.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OAuthProvider {
    GOOGLE, NAVER, KAKAO;

    @JsonCreator
    public static OAuthProvider fromString(String value) {
        return OAuthProvider.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toUpperCase();
    }
}
