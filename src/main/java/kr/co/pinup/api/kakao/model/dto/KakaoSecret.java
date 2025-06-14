package kr.co.pinup.api.kakao.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoSecret(
        @JsonProperty("kakao.api.key.rest")
        String restApiKey,
        @JsonProperty("kakao.api.key.js")
        String jsApiKey
) {
}
