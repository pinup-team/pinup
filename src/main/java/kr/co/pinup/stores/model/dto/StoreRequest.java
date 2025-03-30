package kr.co.pinup.stores.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.pinup.store_operatingHour.model.dto.OperatingHourRequest;

import java.time.LocalDate;
import java.util.List;

public record StoreRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Long categoryId,
        @NotNull Long locationId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Integer thumbnailImage,
        String contactNumber,
        String websiteUrl,
        String snsUrl,
        List<OperatingHourRequest> operatingHours
) { }