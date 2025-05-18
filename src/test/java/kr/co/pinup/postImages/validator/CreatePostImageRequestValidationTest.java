package kr.co.pinup.postImages.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePostImageRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("이미지가 null이면 예외 발생")
    void shouldFail_whenImagesIsNull() {
        CreatePostImageRequest request = CreatePostImageRequest.builder()
                .images(null)
                .build();

        Set<ConstraintViolation<CreatePostImageRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getMessage().equals("이미지는 필수입니다."));
    }

    @Test
    @DisplayName("이미지가 1장일 경우 예외 발생")
    void shouldFail_whenImagesLessThanMin() {
        MockMultipartFile file = new MockMultipartFile("image", "image1.jpg", "image/jpeg", new byte[1]);
        CreatePostImageRequest request = CreatePostImageRequest.builder()
                .images(List.of(file))
                .build();

        Set<ConstraintViolation<CreatePostImageRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getMessage().contains("최소 2장"));
    }

    @Test
    @DisplayName("이미지가 6장일 경우 예외 발생")
    void shouldFail_whenImagesExceedMax() {
        List<MultipartFile> images = List.of(
                mockImage(), mockImage(), mockImage(),
                mockImage(), mockImage(), mockImage() // 6장
        );

        CreatePostImageRequest request = CreatePostImageRequest.builder()
                .images(images)
                .build();

        Set<ConstraintViolation<CreatePostImageRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v -> v.getMessage().contains("최대 5장"));
    }

    @Test
    @DisplayName("이미지가 2~5장일 경우 통과")
    void shouldPass_whenImagesAreValid() {
        List<MultipartFile> images = List.of(
                mockImage(), mockImage()
        );

        CreatePostImageRequest request = CreatePostImageRequest.builder()
                .images(images)
                .build();

        Set<ConstraintViolation<CreatePostImageRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    private MockMultipartFile mockImage() {
        return new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[10]);
    }
}

