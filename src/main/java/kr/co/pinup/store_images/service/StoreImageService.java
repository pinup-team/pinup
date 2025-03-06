package kr.co.pinup.store_images.service;

import kr.co.pinup.store_images.exception.StoreImageNotFoundException;
import kr.co.pinup.store_images.repository.StoreImageRepository;
import kr.co.pinup.stores.StoreImage;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
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

    public List<String> getStoreImages(Long storeId) {
        List<StoreImage> images = storeImageRepository.findByStoreId(storeId);
        return images.stream().map(StoreImage::getImageUrl).collect(Collectors.toList());
    }

    @Transactional
    public List<String> uploadStoreImages(Store store, List<MultipartFile> imageFiles) {
        return  imageFiles.stream().map(imageFile -> {
            String imageUrl = s3Service.uploadFile(imageFile, PATH_PREFIX);

            StoreImage storeImage = StoreImage.builder()
                    .store(store)
                    .imageUrl(imageUrl)
                    .filename(imageFile.getOriginalFilename())
                    .build();

            storeImageRepository.save(storeImage);
            return imageUrl;
        }).collect(Collectors.toList());
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
}
