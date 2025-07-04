package kr.co.pinup.stores.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;

import java.time.LocalDate;
import java.util.List;

public record StoreRequest(
        @NotBlank
        String name,

        @NotBlank
        String description,

        @NotNull
        Long categoryId,

        @NotNull
        Long locationId,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        @Min(0)
        int thumbnailIndex,

        String websiteUrl,

        String snsUrl,

        @Size(min = 1)
        @Valid
        List<StoreOperatingHourRequest> operatingHours
) {

}