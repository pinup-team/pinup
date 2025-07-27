package kr.co.pinup.locations.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CreateLocationRequest(

        @NotBlank(message = "우편번호는 필수 입력값입니다.")
        String zonecode,

        @NotBlank(message = "도/특별시/광역시는 필수 입력값입니다.")
        String sido,

        @NotBlank(message = "시/군/구는 필수 입력값입니다.")
        String sigungu,

        @NotBlank(message = "주소는 필수 입력값입니다.")
        String address,

        String addressDetail
) {
}
