package kr.co.pinup.stores.model.dto;

import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;

import java.time.LocalDateTime;

public record StoreResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        Long locationId,
        String locationName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Status status,
        String imageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getDescription(),
                store.getCategory().getId(),
                store.getCategory().getName(),
                store.getLocation().getId(),
                store.getLocation().getName(),
                store.getStartDate(),
                store.getEndDate(),
                store.getStatus(),
                store.getImageUrl(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}
