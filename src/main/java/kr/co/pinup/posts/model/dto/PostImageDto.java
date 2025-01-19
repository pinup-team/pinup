package kr.co.pinup.posts.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostImageDto {
    private Long postId;

    private List<MultipartFile> images;

    private List<String> imagesToDelete;


    public PostImageDto(List<MultipartFile> images) {
        this.images = images != null ? images : new ArrayList<>();
        this.imagesToDelete = new ArrayList<>();
    }


    public PostImageDto(List<MultipartFile> images, List<String> imagesToDelete) {
        this.images = images != null ? images : new ArrayList<>();
        this.imagesToDelete = imagesToDelete != null ? imagesToDelete : new ArrayList<>();
    }

}
