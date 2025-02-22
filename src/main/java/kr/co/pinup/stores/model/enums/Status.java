package kr.co.pinup.stores.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    PENDING("진행 예정"),
    RESOLVED("진행 중"),
    DISMISSED("종료됨");

    private final String description;

    Status(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return this.description;
    }

    @JsonCreator
    public static Status from(String value) {
        for (Status status : Status.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown value '" + value);
    }

}
