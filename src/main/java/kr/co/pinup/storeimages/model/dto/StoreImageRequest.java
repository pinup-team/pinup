package kr.co.pinup.storeimages.model.dto;

import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Builder
public record StoreImageRequest(List<MultipartFile> images, List<String> imagesToDelete) {
}
