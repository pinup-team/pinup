package kr.co.pinup.comments.model.dto;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;


@Builder
public record CommentResponse(
        Long id,
        Long postId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {

}
