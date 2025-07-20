package kr.co.pinup.storeimages.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.storeimages.exception.StoreImageNotFoundException;
import kr.co.pinup.storeimages.exception.StoreThumbnaiImagelNotFoundException;
import kr.co.pinup.storeimages.model.dto.StoreImageResponse;
import kr.co.pinup.storeimages.repository.StoreImageRepository;
import kr.co.pinup.stores.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreImageService {

    private static final String PATH_PREFIX = "store";

    private final S3Service s3Service;
    private final StoreImageRepository storeImageRepository;

    public List<StoreImageResponse> getStoreImages(Long storeId) {
        List<StoreImage> images = findByStoreId(storeId);

        return images.stream()
                .map(StoreImageResponse::from)
                .collect(Collectors.toList());
    }

    public StoreImageResponse getStoreThumbnailImage(Long storeId) {
        final StoreImage storeImage = storeImageRepository.findByStoreIdAndIsThumbnailTrue(storeId)
                .orElseThrow(StoreThumbnaiImagelNotFoundException::new);

        return StoreImageResponse.from(storeImage);
    }

    @Transactional
    public List<StoreImage> uploadImages(final Store store, final List<MultipartFile> images, int thumbnailIndex) {
        final List<String> uploadUrls = s3UploadFiles(images);

        final List<StoreImage> storeImages = IntStream.range(0, uploadUrls.size())
                .mapToObj(i -> StoreImage.builder()
                        .imageUrl(uploadUrls.get(i))
                        .isThumbnail(i == thumbnailIndex)
                        .store(store)
                        .build())
                .toList();

        storeImageRepository.saveAll(storeImages);

        return storeImages;
    }

    @Transactional
    public void deleteStoreImage(Long storeId) {
        List<StoreImage> images = findByStoreId(storeId);

        images.forEach(image -> {
            String fileName = s3Service.extractFileName(image.getImageUrl());
            s3Service.deleteFromS3(fileName);
        });

        storeImageRepository.deleteAll(images);
    }

    private List<StoreImage> findByStoreId(final Long storeId) {
        List<StoreImage> images = storeImageRepository.findByStoreId(storeId);
        if (images.isEmpty()) {
            throw new StoreImageNotFoundException();
        }
        return images;
    }

    private List<String> s3UploadFiles(List<MultipartFile> files) {
        return files.stream()
                .map(file -> s3Service.uploadFile(file, PATH_PREFIX))
                .collect(Collectors.toList());
    }
}
