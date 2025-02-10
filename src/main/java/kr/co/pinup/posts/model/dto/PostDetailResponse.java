package kr.co.pinup.posts.model.dto;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.posts.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PostDetailResponse(Long id, Long storeId, Long userId, String title, String content,
                                 String thumbnail, LocalDateTime createdAt, LocalDateTime updatedAt,
                                 List<CommentResponse> comments, List<PostImageResponse> postImages) {

    public static PostDetailResponse from(Post post, List<CommentResponse> comments, List<PostImageResponse> images) {
        return new PostDetailResponse(post.getId(), post.getStoreId(), post.getUserId(),
                post.getTitle(), post.getContent(), post.getThumbnail(),
                post.getCreatedAt(), post.getUpdatedAt(), comments, images);
    }
}
