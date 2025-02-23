package kr.co.pinup.comments.model.dto;

import kr.co.pinup.members.Member;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CommentResponse(
        Long id,
        Long postId,
        Member member,
        String content,
        LocalDateTime createdAt
) {

}
