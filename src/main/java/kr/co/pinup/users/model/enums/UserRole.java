package kr.co.pinup.users.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum UserRole {
    ROLE_ADMIN, ROLE_USER;

    @JsonCreator
    public static UserRole fromString(String value) {
        return UserRole.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toUpperCase();
    }
}
