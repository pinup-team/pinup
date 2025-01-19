package kr.co.pinup.posts.model.dto;

import jakarta.validation.constraints.NotEmpty;
import kr.co.pinup.posts.model.entity.PostEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long postId;
    private Long userId;

    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;

    public CommentDto(Long id, PostEntity post, Long userId, String content) {
    }
}