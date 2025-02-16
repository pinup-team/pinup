package kr.co.pinup.stores.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.pinup.stores.model.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StoreRequest(
        @NotBlank String name,
        @NotBlank String description,
        Long categoryId,
        Long locationId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String imageUrl
) {
}