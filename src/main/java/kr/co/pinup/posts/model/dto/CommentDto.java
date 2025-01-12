package kr.co.pinup.posts.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long postId;    // 게시글 ID
    private Long userId;    // 작성자 ID

    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;      // 게시글 내용
}