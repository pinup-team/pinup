package kr.co.pinup.postImages.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostImageRequest implements PostImageUploadRequest {
    private List<MultipartFile> images;
    private List<String> imagesToDelete;
}
