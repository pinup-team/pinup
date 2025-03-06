package kr.co.pinup.stores.model.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        String imageUrl
) {
    @JsonCreator
    public StoreRequest(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("categoryId") Long categoryId,
            @JsonProperty("locationId") Long locationId,
            @JsonProperty("startDate") LocalDate startDate,
            @JsonProperty("endDate") LocalDate endDate,
            @JsonProperty("imageUrl") String imageUrl
    ) {
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.locationId = locationId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.imageUrl = imageUrl;
    }
}