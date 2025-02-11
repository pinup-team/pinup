package kr.co.pinup.stores.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.pinup.stores.model.enums.Status;

import java.time.LocalDateTime;

public record StoreRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Long categoryId,
        @NotNull Long locationId,
        @NotNull LocalDateTime startDate,
        @NotNull LocalDateTime endDate,
        @NotNull Status status,
        @NotBlank String imageUrl
) {
}