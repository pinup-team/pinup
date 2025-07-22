package kr.co.pinup.stores.model.dto;

import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.storecategories.model.dto.StoreCategoryResponse;
import kr.co.pinup.storeimages.model.dto.StoreImageResponse;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourResponse;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record StoreResponse(
        Long id,
        String name,
        String description,
        StoreStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String websiteUrl,
        String snsUrl,
        long viewCount,
        StoreCategoryResponse category,
        LocationResponse location,
        List<StoreOperatingHourResponse> operatingHours,
        List<StoreImageResponse> storeImages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getDescription(),
                store.getStoreStatus(),
                store.getStartDate(),
                store.getEndDate(),
                store.getWebsiteUrl(),
                store.getSnsUrl(),
                store.getViewCount(),
                StoreCategoryResponse.from(store.getCategory()),
                LocationResponse.from(store.getLocation()),
                store.getOperatingHours().stream()
                        .map(o -> new StoreOperatingHourResponse(
                                o.getDays(), o.getStartTime(), o.getEndTime()
                        )).toList(),
                store.getStoreImages().stream()
                        .filter(storeImage -> !storeImage.isDeleted())
                        .map(StoreImageResponse::from)
                        .toList(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}