package kr.co.pinup.stores.model.dto;

import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.store_categories.model.dto.StoreCategoryResponse;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StoreResponse(
        Long id,
        String name,
        String description,
        StoreCategoryResponse category,
        LocationResponse location,
        LocalDate startDate,
        LocalDate endDate,
        StoreStatus storeStatus,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
                store.getStoreStatus(),
                store.getImageUrl(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }

}
