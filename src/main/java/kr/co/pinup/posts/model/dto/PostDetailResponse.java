package kr.co.pinup.posts.model.dto;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.posts.Post;
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

    public static PostDetailResponse postDetailResponse(Post post, List<CommentResponse> comments, List<PostImageResponse> images) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .storeId(post.getStoreId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .thumbnail(post.getThumbnail())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(comments)
                .postImages(images)
                .build();
    }

}
