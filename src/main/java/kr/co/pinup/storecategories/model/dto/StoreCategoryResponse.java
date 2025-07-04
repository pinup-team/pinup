package kr.co.pinup.storecategories.model.dto;

import kr.co.pinup.storecategories.StoreCategory;

public record StoreCategoryResponse(Long id, String name) {

    public static StoreCategoryResponse from(StoreCategory storeCategory) {
        return new StoreCategoryResponse(
                storeCategory.getId(), storeCategory.getName()
        );
    }

}
