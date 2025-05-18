package kr.co.pinup.comments.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CreateCommentRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("댓글 내용이 null일 경우 검증 실패")
    void shouldFail_whenContentIsNull() {
        CreateCommentRequest request = new CreateCommentRequest(null);

        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getMessage().equals("내용을 입력해주세요."));
    }

    @Test
    @DisplayName("댓글 내용이 공백일 경우 검증 실패")
    void shouldFail_whenContentIsEmpty() {
        CreateCommentRequest request = new CreateCommentRequest("");

        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getMessage().equals("내용을 입력해주세요."));
    }

    @Test
    @DisplayName("댓글 내용이 유효한 경우 검증 통과")
    void shouldPass_whenContentIsValid() {
        CreateCommentRequest request = new CreateCommentRequest("정상 댓글입니다.");

        Set<ConstraintViolation<CreateCommentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}