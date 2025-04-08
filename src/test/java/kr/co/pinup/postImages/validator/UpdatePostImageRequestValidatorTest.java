package kr.co.pinup.postImages.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UpdatePostImageRequestValidatorTest {
    private List<String> existingImages;

    @BeforeEach
    void setUp() {
        existingImages = new ArrayList<>(List.of(
                "img1.png", "img2.png", "img3.png"
        ));
    }

    private boolean isValid(List<String> imagesToDelete, List<MultipartFile> newImages) {
        int deletedCount = (imagesToDelete != null) ? imagesToDelete.size() : 0;
        int addedCount = (newImages != null) ? (int) newImages.stream().filter(f -> !f.isEmpty()).count() : 0;

        int remaining = existingImages.size() - deletedCount + addedCount;
        return remaining >= 2;
    }

    @Test
    @DisplayName("삭제 + 추가 이미지 조합 결과가 2장 이상이면 검증 통과")
    void shouldPass_whenRemainingImagesAreTwoOrMore() {
        // given
        List<String> toDelete = List.of("img1.png");
        List<MultipartFile> newImages = List.of(mockFile("new1.png"));

        // expect
        assertThat(isValid(toDelete, newImages)).isTrue();
    }

    @Test
    @DisplayName("삭제 + 추가 이미지 조합 결과가 2장 미만이면 검증 실패")
    void shouldFail_whenRemainingImagesAreLessThanTwo() {
        // given
        List<String> toDelete = List.of("img1.png", "img2.png", "img3.png");
        List<MultipartFile> newImages = List.of();

        // expect
        assertThat(isValid(toDelete, newImages)).isFalse();
    }

    private MultipartFile mockFile(String name) {
        return new MockMultipartFile("images", name, "image/png", new byte[10]);
    }
}
