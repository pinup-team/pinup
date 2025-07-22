package kr.co.pinup.storeoperatinghour.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record StoreOperatingHourRequest(
        @NotBlank(message = "팝업스토어 운영 요일은 필수값입니다.")
        String days,
        @NotNull(message = "팝업스토어 오픈 시간은 필수값입니다.")
        LocalTime startTime,
        @NotNull(message = "팝업스토어 마감 시간은 필수값입니다.")
        LocalTime endTime
) {
}
