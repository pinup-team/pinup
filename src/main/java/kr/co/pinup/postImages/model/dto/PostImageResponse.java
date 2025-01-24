package kr.co.pinup.postImages.model.dto;

import kr.co.pinup.postImages.PostImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageResponse {
    private Long id;
    private Long postId;
    private String s3Url;

    public static List<PostImageResponse> fromList(List<PostImageResponse> images) {
        return images.stream()
                .map(image -> new PostImageResponse(image.getId(), image.getPostId(), image.getS3Url()))
                .collect(Collectors.toList());
    }
    public static PostImageResponse from(PostImage postImage) {
        return new PostImageResponse(postImage.getId(), postImage.getPost().getId(), postImage.getS3Url());
    }

}
