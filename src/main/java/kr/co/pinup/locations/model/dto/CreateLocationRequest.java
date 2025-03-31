package kr.co.pinup.locations.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateLocationRequest(
        @NotBlank(message = "이름은 필수 입력값입니다.")
        String name,

        @NotBlank(message = "우편번호는 필수 입력값입니다.")
        String zoneCode,

        @NotBlank(message = "도/특별시/광역시는 필수 입력값입니다.")
        String state,

        @NotBlank(message = "시/군/구는 필수 입력값입니다.")
        String district,

        @NotNull(message = "위도(latitude)는 필수 입력값입니다.")
        Double latitude,

        @NotNull(message = "경도(longitude)는 필수 입력값입니다.")
        Double longitude,

        @NotBlank(message = "주소는 필수 입력값입니다.")
        String address,

        String addressDetail
) {
}
