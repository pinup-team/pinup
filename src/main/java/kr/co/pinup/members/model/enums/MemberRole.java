package kr.co.pinup.members.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberRole {
    ROLE_ADMIN, ROLE_USER;

    @JsonCreator
    public static MemberRole fromString(String value) {
        return MemberRole.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toUpperCase();
    }
}
