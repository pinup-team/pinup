package kr.co.pinup.stores.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StoreStatus {
    PENDING("진행 예정"),
    RESOLVED("진행 중"),
    DISMISSED("종료됨");

    private final String description;

    StoreStatus(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return this.description;
    }

    @JsonCreator
    public static StoreStatus from(String value) {
        for (StoreStatus storeStatus : StoreStatus.values()) {
            if (storeStatus.name().equalsIgnoreCase(value)) {
                return storeStatus;
            }
        }
        throw new IllegalArgumentException("Unknown value '" + value);
    }

}
