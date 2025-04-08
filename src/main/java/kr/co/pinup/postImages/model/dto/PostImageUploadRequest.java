package kr.co.pinup.postImages.model.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostImageUploadRequest {
    List<MultipartFile> getImages();
}
