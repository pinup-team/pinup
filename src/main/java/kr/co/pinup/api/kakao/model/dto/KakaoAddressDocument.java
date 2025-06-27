package kr.co.pinup.api.kakao.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record KakaoAddressDocument(
        @JsonProperty("address_name")
        String addressName,
        @JsonProperty("x")
        double longitude,
        @JsonProperty("y")
        double latitude
) {
        public KakaoAddressDocument {
                longitude = roundGeo(longitude);
                latitude = roundGeo(latitude);
        }

        private double roundGeo(final double value) {
                return Math.round(value * 1_000_000d) / 1_000_000d;
        }
}
