package kr.co.pinup.posts.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CreatePostRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("storeId가 null이면 실패한다")
    void shouldFail_whenStoreIdIsNull() {
        CreatePostRequest request = CreatePostRequest.builder()
                .storeId(null)
                .title("제목")
                .content("내용")
                .build();

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("스토어 ID는 필수 값입니다."));
    }

    @Test
    @DisplayName("storeId가 0 이하일 경우 실패한다")
    void shouldFail_whenStoreIdIsNotPositive() {
        CreatePostRequest request = CreatePostRequest.builder()
                .storeId(0L)
                .title("제목")
                .content("내용")
                .build();

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("스토어 ID는 양수여야 합니다."));
    }

    @Test
    @DisplayName("제목이 공백일 경우 유효성 검증에 실패한다")
    void shouldFail_whenTitleIsBlank() {
        CreatePostRequest request = CreatePostRequest.builder()
                .storeId(1L)
                .title(" ")
                .content("내용")
                .build();

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("제목을 입력해주세요."));
    }

    @Test
    @DisplayName("내용이 공백일 경우 유효성 검증에 실패한다")
    void shouldFail_whenContentIsBlank() {
        CreatePostRequest request = CreatePostRequest.builder()
                .storeId(1L)
                .title("제목")
                .content(" ")
                .build();

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("내용을 입력해주세요."));
    }

    @Test
    @DisplayName("제목이 100자를 초과할 경우 유효성 검증에 실패한다")
    void shouldFail_whenTitleExceedsMaxLength() {
        String longTitle = "a".repeat(101);
        CreatePostRequest request = CreatePostRequest.builder()
                .storeId(1L)
                .title(longTitle)
                .content("내용")
                .build();

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("제목은 1자 이상, 100자 이하로 입력해주세요."));
    }

    @Test
    @DisplayName("내용이 2000자를 초과할 경우 유효성 검증에 실패한다")
    void shouldFail_whenContentExceedsMaxLength() {
        String longContent = "a".repeat(2001);
        CreatePostRequest request = CreatePostRequest.builder()
                .storeId(1L)
                .title("제목")
                .content(longContent)
                .build();

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().equals("내용은 1자 이상, 2000자 이하로 입력해주세요."));
    }

    @Test
    @DisplayName("모든 입력값이 유효한 경우 유효성 검증에 통과한다")
    void shouldPass_whenValid() {
        CreatePostRequest request = CreatePostRequest.builder()
                .storeId(1L)
                .title("정상 제목")
                .content("정상 내용")
                .build();

        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}