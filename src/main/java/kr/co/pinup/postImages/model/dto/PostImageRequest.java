package kr.co.pinup.postImages.model.dto;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageRequest {

    private Long postId;
    private List<MultipartFile> images;
    private List<String> imagesToDelete;
}
