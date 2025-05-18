package kr.co.pinup.posts.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CreatePostRequest(
        @NotNull(message = "스토어 ID는 필수 값입니다.")
        @Positive(message = "스토어 ID는 양수여야 합니다.")
        Long storeId,

        @NotBlank(message = "제목을 입력해주세요.")
        @Size(min = 1, max = 100, message = "제목은 1자 이상, 100자 이하로 입력해주세요.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(min = 1, max = 2000, message = "내용은 1자 이상, 2000자 이하로 입력해주세요.")
        String content
) {
}
