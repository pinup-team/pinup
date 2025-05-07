package kr.co.pinup.stores.valid;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import kr.co.pinup.store_operatingHour.model.dto.OperatingHourRequest;
import kr.co.pinup.stores.model.dto.StoreRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StoreRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
//        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    }

    @Test
    @DisplayName("유효한 StoreRequest")
    void validStoreRequest() {
        StoreRequest request = new StoreRequest(
                "배민 계란프라이 데이",
                "장보기도, 도전도, 선물도 한-계란 없는 날! \uD83E\uDD5A+\uD83E\uDD5A",
                1L,
                1L,
                LocalDate.of(2025, 6, 9),
                LocalDate.of(2025, 6, 11),
                0,
                "010-0000-0000",
                "https://example.com",
                "https://www.instagram.com/baemin_official/",
                List.of(new OperatingHourRequest("월~금", LocalTime.of(10, 30), LocalTime.of(20, 0))));

        Set<ConstraintViolation<StoreRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "유효한 StoreRequest");

    }

    @Test
    @DisplayName("유효하지 않은 StoreRequest - null값 입력")
    void invalidStoreRequestWithNullFields() {
        StoreRequest request = new StoreRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "010-0000-0000",
                "https://example.com",
                "https://www.instagram.com/baemin_official/",
                null

        );

        Set<ConstraintViolation<StoreRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "필수 필드 null");

    }

    @Test
    @DisplayName("유효하지 않은 StoreRequest - Blank 입력")
    void invalidStoreRequestWithBlankFields() {
        StoreRequest request = new StoreRequest(
                "",
                "",
                1L,
                1L,
                LocalDate.of(2025, 6, 9),
                LocalDate.of(2025, 6, 11),
                0,
                "010-0000-0000",
                "https://example.com",
                "https://www.instagram.com/baemin_official/",
                List.of(new OperatingHourRequest("월~금", LocalTime.of(10, 30), LocalTime.of(20, 0))));

        Set<ConstraintViolation<StoreRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "필수 필드 blank");

    }
}
