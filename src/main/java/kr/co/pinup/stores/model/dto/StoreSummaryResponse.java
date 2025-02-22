package kr.co.pinup.stores.model.dto;

import kr.co.pinup.stores.Store;

public record StoreSummaryResponse(
        Long id,
        String name,
        String state,
        String categoryName,
        String imageUrl
) {
    public static StoreSummaryResponse from(Store store) {
        return new StoreSummaryResponse(
                store.getId(),
                store.getName(),
                store.getLocation().getState(),
                store.getCategory().getName(),
                store.getImageUrl()
        );
    }
}