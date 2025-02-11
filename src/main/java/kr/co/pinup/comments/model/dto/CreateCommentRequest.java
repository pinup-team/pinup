package kr.co.pinup.comments.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record CreateCommentRequest(
        Long postId,
        Long userId,

        @NotEmpty(message = "내용을 입력해주세요.")
        String content
) {
}

