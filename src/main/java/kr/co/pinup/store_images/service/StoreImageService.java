package kr.co.pinup.store_images.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.store_images.StoreImage;
import kr.co.pinup.store_images.exception.StoreImageNotFoundException;
import kr.co.pinup.store_images.exception.StoreImageSaveFailedException;
import kr.co.pinup.store_images.model.dto.StoreImageRequest;
import kr.co.pinup.store_images.model.dto.StoreImageResponse;
import kr.co.pinup.store_images.repository.StoreImageRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreImageService {

    private final S3Service s3Service;
    private final StoreRepository storeRepository;
    private static final String PATH_PREFIX = "store";
    private final StoreImageRepository storeImageRepository;

    public List<StoreImageResponse> getStoreImages(Long storeId) {
        List<StoreImage> images = storeImageRepository.findByStoreId(storeId);
        return images.stream()
                .map(StoreImageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<StoreImage> uploadStoreImages(Store store, StoreImageRequest storeImageRequest) {

        List<String> imageUrls = uploadFiles(storeImageRequest.getImages(), PATH_PREFIX);

        List<StoreImage> storeImages = imageUrls.stream()
                .map(s3Url -> new StoreImage(store, s3Url))
                .collect(Collectors.toList());

        try {
            storeImageRepository.saveAll(storeImages);
        } catch (Exception e) {
            throw new StoreImageSaveFailedException("이미지 저장 중 문제가 발생했습니다.", e);
        }

        return storeImages;

    }

    @Transactional
    public void deleteStoreImage(Long storeId) {
       List<StoreImage> images = storeImageRepository.findByStoreId(storeId);

       images.forEach(image -> {
                           String fileName = s3Service.extractFileName(image.getImageUrl());
                           s3Service.deleteFromS3(fileName);
                       });

        storeImageRepository.deleteAll(images);
    }

    public String getStoreImage(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreImageNotFoundException("스토어를 찾을 수 없습니다."));
        return store.getImageUrl();
    }

    public List<String> uploadFiles(List<MultipartFile> files, String pathPrefix) {
        return files.stream()
                .map(file -> s3Service.uploadFile(file, pathPrefix))
                .collect(Collectors.toList());
    }
}
