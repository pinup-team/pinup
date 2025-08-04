package kr.co.pinup.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OAuthProvider {
    GOOGLE("구글"),
    NAVER("네이버"),
    KAKAO("카카오"),
    PINUP("일반 로그인");

    private final String displayName;

    OAuthProvider(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static OAuthProvider fromString(String value) {
        return OAuthProvider.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toUpperCase();
    }
}
