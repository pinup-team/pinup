package kr.co.pinup.postLikes.model.dto;

public record PostLikeResponse(
        int likeCount,
        boolean likedByCurrentUser
) {
    public static PostLikeResponse of(int likeCount, boolean liked) {
        return new PostLikeResponse(likeCount, liked);
    }

}