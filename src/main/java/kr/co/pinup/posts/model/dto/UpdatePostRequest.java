package kr.co.pinup.posts.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdatePostRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(min = 1, max = 100, message = "제목은 1자 이상, 100자 이하로 입력해주세요.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(min = 1, max = 2000, message = "내용은 1자 이상, 2000자 이하로 입력해주세요.")
        String content
) {
}