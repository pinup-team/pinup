package kr.co.pinup.stores.model.dto;

import kr.co.pinup.stores.Store;

import java.time.LocalDate;

public record StoreSummaryResponse(
        Long id,
        String name,
        String district,
        String categoryName,
        LocalDate startDate,
        LocalDate endDate,
        String imageUrl
) {
    public static StoreSummaryResponse from(Store store) {
        return new StoreSummaryResponse(
                store.getId(),
                store.getName(),
                store.getLocation().getDistrict(),
                store.getCategory().getName(),
                store.getStartDate(),
                store.getEndDate(),
                store.getImageUrl()
        );
    }

}