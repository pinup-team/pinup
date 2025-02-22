package kr.co.pinup.stores.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StoreRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Long categoryId,
        @NotNull Long locationId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank String imageUrl
) {
}