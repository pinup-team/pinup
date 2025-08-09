package kr.co.pinup.stores.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum StoreStatus {

    RESOLVED("진행 중"),
    PENDING("진행 예정"),
    DISMISSED("종료됨");

    private final String value;

    public static StoreStatus from(String status) {
        return Arrays.stream(StoreStatus.values())
                .filter(storeStatus -> storeStatus.name().equalsIgnoreCase(status))
                .findFirst()
                .orElse(null);
    }

}
