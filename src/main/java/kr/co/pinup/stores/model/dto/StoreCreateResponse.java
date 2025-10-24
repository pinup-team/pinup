package kr.co.pinup.stores.model.dto;

import kr.co.pinup.stores.Store;

import java.time.LocalDateTime;

public record StoreCreateResponse(Long id, LocalDateTime createdAt) {
    public static StoreCreateResponse from(Store store) {
        return new StoreCreateResponse(store.getId(), store.getCreatedAt());
    }
}