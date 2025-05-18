package kr.co.pinup.store_categories.model.dto;

import kr.co.pinup.store_categories.StoreCategory;

public record StoreCategoryResponse(
        Long id,
        String name
) {
    public static StoreCategoryResponse from(StoreCategory storeCategory) {
        return new StoreCategoryResponse(
                storeCategory.getId(),
                storeCategory.getName()
        );
    }

}
