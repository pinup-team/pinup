package kr.co.pinup.stores.model.dto;

import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;

import java.time.LocalDate;

public record StoreThumbnailResponse(
        Long id,
        String name,
        StoreStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String categoryName,
        String sigungu,
        String thumbnailImage
) {
    public static StoreThumbnailResponse from(Store store) {
        final String thumbnailUrl = store.getStoreImages().stream()
                .filter(StoreImage::isThumbnail)
                .findFirst()
                .map(StoreImage::getImageUrl)
                .orElse(null);

        return new StoreThumbnailResponse(
                store.getId(),
                store.getName(),
                store.getStoreStatus(),
                store.getStartDate(),
                store.getEndDate(),
                store.getCategory().getName(),
                store.getLocation().getSigungu(),
                thumbnailUrl
        );
    }

}