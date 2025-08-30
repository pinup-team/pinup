package kr.co.pinup.posts.model.dto;

import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.posts.Post;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PostResponse(Long id, Long storeId, MemberResponse member, String title, String content,
                           String thumbnail, LocalDateTime createdAt, LocalDateTime updatedAt
                          ,  int commentCount, int  likeCount, boolean likedByCurrentUser ) {

    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getStore().getId(), new MemberResponse(post.getMember()),
                post.getTitle(), post.getContent(), post.getThumbnail(),
                post.getCreatedAt(), post.getUpdatedAt(), 0, 0, false);
    }

    public PostResponse(Long id,
                        String memberNickname,
                        String title,
                        String thumbnail,
                        LocalDateTime createdAt,
                        Long commentCount,
                        Integer likeCount,
                        Boolean likedByCurrentUser) {
        this(
                id,
                null,
                MemberResponse.ofNickname(memberNickname),
                title,
                 null,
                thumbnail,
                createdAt,
                null,
                commentCount != null ? commentCount.intValue() : 0,
                likeCount != null ? likeCount.intValue() : 0,
                Boolean.TRUE.equals(likedByCurrentUser)
        );
    }

}

