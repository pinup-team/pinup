package kr.co.pinup.posts.model.dto;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record PostDetailResponse(PostResponse post, List<CommentResponse> comments,
                                 List<PostImageResponse> postImages) {

    public static PostDetailResponse from(PostResponse post, List<CommentResponse> comments, List<PostImageResponse> images) {
        return new PostDetailResponse(post, comments, images);
    }
}
