package kr.co.pinup.posts.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
public class PostDto {

    private Long storeId;        // 행사 아이디
    private Long userId;         // 작성자 아이디

    @NotEmpty(message = "제목을 입력해주세요.")
    @Size(min = 1, max = 100, message = "제목은 1자 이상, 100자 이하로 입력해주세요.")
    private String title;        // 게시글 제목

    @NotEmpty(message = "내용을 입력해주세요.")
    private String content;      // 게시글 내용

    private String thumbnail;    // 게시글 썸네일 URL

    private List<MultipartFile> images;   // 게시글과 관련된 이미지 URL들 (PostImageEntity의 s3Url을 전달)

    private List<String> imagesToDelete; // 삭제할 이미지 URL 리스트

}