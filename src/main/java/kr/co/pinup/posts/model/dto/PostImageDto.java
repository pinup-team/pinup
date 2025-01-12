package kr.co.pinup.posts.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageDto {
    private Long postId;      // 이미지가 속한 게시글의 ID

    private List<MultipartFile> images;   // 게시글과 관련된 이미지 URL들 (PostImageEntity의 s3Url을 전달)

    private List<String> imagesToDelete; // 삭제할 이미지 URL 리스트
}
