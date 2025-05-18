package kr.co.pinup.stores.model.dto;

import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.store_categories.model.dto.StoreCategoryResponse;
import kr.co.pinup.store_operatingHour.model.dto.OperatingHourResponse;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StoreResponse(
        Long id,
        String name,
        String description,
        StoreCategoryResponse category,
        LocationResponse location,
        LocalDate startDate,
        LocalDate endDate,
        Status status,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String contactNumber,
        String websiteUrl,
        String snsUrl,
        List<OperatingHourResponse> operatingHours
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getDescription(),
                StoreCategoryResponse.from(store.getCategory()),
                LocationResponse.from(store.getLocation()),
                store.getStartDate(),
                store.getEndDate(),
                store.getStatus(),
                store.getImageUrl(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                store.getContactNumber(),
                store.getWebsiteUrl(),
                store.getSnsUrl(),
                store.getOperatingHours().stream()
                        .map(o -> new OperatingHourResponse(
                                o.getDay(), o.getStartTime(), o.getEndTime()
                        )).toList()
        );
    }
}