package kr.co.pinup.postImages.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostImageRequest implements PostImageUploadRequest {
    @NotEmpty(message = "이미지는 필수입니다.")
    @Size(min = 2, max = 5, message = "이미지는 최소 2장 이상, 최대 5장까지 등록 가능합니다.")
    private List<MultipartFile> images;
}
