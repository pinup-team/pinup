package kr.co.pinup.posts.model.dto;

import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.posts.Post;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostResponse(Long id, Long storeId, MemberResponse member, String title, String content,
                           String thumbnail, LocalDateTime createdAt, LocalDateTime updatedAt,  int commentCount) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getStore().getId(), new MemberResponse(post.getMember()),
                post.getTitle(), post.getContent(), post.getThumbnail(),
                post.getCreatedAt(), post.getUpdatedAt(), 0);
    }

    public static PostResponse fromPostWithComments(Post post, int commentCount) {
        return new PostResponse(post.getId(), post.getStore().getId(), new MemberResponse(post.getMember()),
                post.getTitle(), post.getContent(), post.getThumbnail(),
                post.getCreatedAt(), post.getUpdatedAt(), commentCount);
    }
}

