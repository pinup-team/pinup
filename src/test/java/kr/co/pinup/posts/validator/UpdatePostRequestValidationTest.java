package kr.co.pinup.posts.validator;

import jakarta.validation.*;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdatePostRequestValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("제목이 공백일 경우 유효성 검증에 실패한다")
    void shouldFail_whenTitleIsEmpty() {
        UpdatePostRequest request = new UpdatePostRequest("  ", "내용");

        Set<ConstraintViolation<UpdatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("제목을 입력해주세요."));
    }

    @Test
    @DisplayName("내용이 공백일 경우 유효성 검증에 실패한다")
    void shouldFail_whenContentIsEmpty() {
        UpdatePostRequest request = new UpdatePostRequest("제목", "  ");

        Set<ConstraintViolation<UpdatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("내용을 입력해주세요."));
    }

    @Test
    @DisplayName("제목이 100자를 초과할 경우 유효성 검증에 실패한다")
    void shouldFail_whenTitleIsTooLong() {
        String longTitle = "a".repeat(101);
        UpdatePostRequest request = new UpdatePostRequest(longTitle, "내용");

        Set<ConstraintViolation<UpdatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("제목은 1자 이상, 100자 이하로 입력해주세요."));
    }

    @Test
    @DisplayName("내용이 2000자를 초과할 경우 유효성 검증에 실패한다")
    void shouldFail_whenContentIsTooLong() {
        String longContent = "a".repeat(2001);
        UpdatePostRequest request = new UpdatePostRequest("제목", longContent);

        Set<ConstraintViolation<UpdatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("내용은 1자 이상, 2000자 이하로 입력해주세요."));
    }

    @Test
    @DisplayName("모든 입력값이 유효한 경우 유효성 검증에 통과한다")
    void shouldPass_whenValidInput() {
        UpdatePostRequest request = new UpdatePostRequest("제목", "내용");

        Set<ConstraintViolation<UpdatePostRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}