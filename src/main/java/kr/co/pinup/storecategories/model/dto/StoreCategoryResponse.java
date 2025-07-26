package kr.co.pinup.storecategories.model.dto;

import kr.co.pinup.storecategories.StoreCategory;

import java.time.LocalDateTime;

public record StoreCategoryResponse(Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static StoreCategoryResponse from(StoreCategory storeCategory) {
        return new StoreCategoryResponse(
                storeCategory.getId(),
                storeCategory.getName(),
                storeCategory.getCreatedAt(),
                storeCategory.getUpdatedAt()
        );
    }

}
