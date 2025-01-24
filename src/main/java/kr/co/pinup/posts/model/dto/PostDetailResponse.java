package kr.co.pinup.posts.model.dto;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Builder
public class PostDetailResponse {

    private Long id;
    private Long storeId;
    private Long userId;
    private String title;
    private String content;
    private String thumbnail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<CommentResponse> comments;
    private List<PostImageResponse> postImages;
}
