package kr.co.pinup.stores.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record StoreUpdateRequest(
        @NotBlank(message = "스토어 이름은 필수 입력값입니다.")
        String name,

        @NotBlank(message = "스토어 설명은 필수 입력값입니다.")
        String description,

        @NotNull(message = "시작 날짜는 필수 입력값입니다.")
        LocalDate startDate,

        @NotNull(message = "종료 날짜는 필수 입력값입니다.")
        LocalDate endDate,

        String websiteUrl,

        String snsUrl,

        Long thumbnailId,

        Long thumbnailIndex,

        @NotNull(message = "카테고리는 필수 입력값입니다.")
        Long categoryId,

        @NotNull(message = "위치는 필수 입력값입니다.")
        Long locationId,

        @Size(min = 1)
        @Valid
        List<StoreOperatingHourRequest> operatingHours,

        List<Long> deletedImageIds
) {
}
