package kr.co.pinup.posts.model.dto;

import kr.co.pinup.posts.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostResponse {
    private Long id;
    private Long storeId;
    private Long userId;
    private String title;
    private String content;
    private String thumbnail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getStoreId(), post.getUserId(),
                post.getTitle(), post.getContent(), post.getThumbnail(),
                post.getCreatedAt(), post.getUpdatedAt());
    }
}
