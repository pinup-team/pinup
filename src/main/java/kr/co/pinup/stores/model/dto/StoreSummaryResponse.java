package kr.co.pinup.stores.model.dto;

import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;

import java.time.LocalDate;
import java.util.List;

public record StoreSummaryResponse(
        Long id,
        String name,
        StoreStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String categoryName,
        String district,
        List<StoreImage> storeImages
) {
    public static StoreSummaryResponse from(Store store) {
        return new StoreSummaryResponse(
                store.getId(),
                store.getName(),
                store.getStoreStatus(),
                store.getStartDate(),
                store.getEndDate(),
                store.getCategory().getName(),
                store.getLocation().getSigungu(),
                store.getStoreImages()
        );
    }

}