package kr.co.pinup.comments.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class CreateCommentRequest {
    private Long postId;
    private Long userId;

    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;

}
