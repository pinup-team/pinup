package kr.co.pinup.verification.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VerifyPurpose {
    REGISTER, RESET_PASSWORD;

    @JsonCreator
    public static VerifyPurpose fromString(String value) {
        return VerifyPurpose.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toJson() {
        return this.name().toUpperCase();
    }
}
