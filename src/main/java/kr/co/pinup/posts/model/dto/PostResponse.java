package kr.co.pinup.posts.model.dto;

import kr.co.pinup.posts.Post;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostResponse(Long id, Long storeId, Long userId, String title, String content,
                           String thumbnail, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getStoreId(), post.getUserId(),
                post.getTitle(), post.getContent(), post.getThumbnail(),
                post.getCreatedAt(), post.getUpdatedAt());
    }
}

