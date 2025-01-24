package kr.co.pinup.posts.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import kr.co.pinup.postImages.model.dto.PostImageRequest;


import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {
    private Long postId;
    private Long storeId;
    private Long userId;

    @NotEmpty(message = "제목을 입력해주세요.")
    @Size(min = 1, max = 100, message = "제목은 1자 이상, 100자 이하로 입력해주세요.")
    private String title;

    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;

    private String thumbnail;

    private PostImageRequest postImageRequest;

}
