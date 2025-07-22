package kr.co.pinup.storeimages.service;

import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.locations.Location;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.storeimages.exception.StoreImageNotFoundException;
import kr.co.pinup.storeimages.exception.StoreThumbnaiImagelNotFoundException;
import kr.co.pinup.storeimages.model.dto.StoreImageResponse;
import kr.co.pinup.storeimages.repository.StoreImageRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class StoreImageServiceTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private StoreImageRepository storeImageRepository;

    @InjectMocks
    private StoreImageService storeImageService;

    @DisplayName("Store 아이디에 해당하는 스토어 이미지들을 반환한다.")
    @Test
    void getStoreImages() {
        // Arrange
        final Long storeId = 1L;
        final StoreImage storeImage = getStoreImage("http://127.0.0.1:4566/pinup/store/image.png", true);

        given(storeImageRepository.findByStoreIdAndIsDeletedFalse(storeId)).willReturn(List.of(storeImage));


        // Act
        final List<StoreImageResponse> result = storeImageService.getStoreImages(storeId);

        // Assert
        assertThat(result).hasSize(1)
                .extracting("imageUrl")
                .containsExactlyInAnyOrder(storeImage.getImageUrl());

        then(storeImageRepository).should(times(1))
                .findByStoreIdAndIsDeletedFalse(storeId);
    }

    @DisplayName("존재하지 않는 Store 아이디로 스토어 이미지를 조회하면 예외가 발생한다.")
    @Test
    void getStoreImagesWithNonExistStoreId() {
        // Arrange
        final long storeId = Long.MAX_VALUE;

        given(storeImageRepository.findByStoreIdAndIsDeletedFalse(storeId)).willReturn(List.of());

        // Act Assert
        assertThatThrownBy(() -> storeImageService.getStoreImages(storeId))
                .isInstanceOf(StoreImageNotFoundException.class)
                .hasMessage("해당 스토어 ID에 이미지가 존재하지 않습니다.");
    }

    @DisplayName("Store 아이디에 해당하는 스토어 썸네일 이미지를 반환한다.")
    @Test
    void getStoreThumbnailImage() {
        // Arrange
        final long storeId = 1L;
        final StoreImage storeImage = getStoreImage("http://127.0.0.1:4566/pinup/store/image.png", true);

        given(storeImageRepository.findByStoreIdAndIsThumbnailTrueAndIsDeletedFalse(storeId))
                .willReturn(Optional.ofNullable(storeImage));

        // Act
        final StoreImageResponse result = storeImageService.getStoreThumbnailImage(storeId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.imageUrl()).isEqualTo(storeImage.getImageUrl());

        then(storeImageRepository).should(times(1))
                .findByStoreIdAndIsThumbnailTrueAndIsDeletedFalse(storeId);
    }

    @DisplayName("존재하지 않는 Store 아이디로 썸네일 이미지를 조회하면 예외가 발생한다.")
    @Test
    void getStoreThumbnailImageWithNonExistStoreId() {
        // Arrange
        final long storeId = Long.MAX_VALUE;

        given(storeImageRepository.findByStoreIdAndIsThumbnailTrueAndIsDeletedFalse(storeId))
                .willThrow(new StoreThumbnaiImagelNotFoundException());

        // Act Assert
        assertThatThrownBy(() -> storeImageService.getStoreThumbnailImage(storeId))
                .isInstanceOf(StoreThumbnaiImagelNotFoundException.class)
                .hasMessage("해당 스토어 ID에 썸네일 이미지가 존재하지 않습니다.");
    }

    @DisplayName("S3에 이미지를 업로드하고 스토어 이미지들을 저장한다.")
    @Test
    void createUploadImages() {
        // Arrange
        final Store store = getStore(getStoreCategory(), getLocation());
        final List<MultipartFile> images = List.of(mock(MultipartFile.class));
        final String uploadUrl = "http://127.0.0.1:4566/pinup/store/image.png";

        given(s3Service.uploadFile(any(MultipartFile.class), anyString())).willReturn(uploadUrl);

        // Act
        final List<StoreImage> result = storeImageService.createUploadImages(store, images, 0L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getImageUrl()).isEqualTo(uploadUrl);
    }

    @DisplayName("썸네일을 변경한다")
    @Test
    void updateThumbnailImage() {
        // Arrange
        final long storeId = 1L;
        final long thumbnailId = 2L;

        final StoreImage mockStoreImage1 = mock(StoreImage.class);
        final StoreImage mockStoreImage2 = mock(StoreImage.class);

        given(storeImageRepository.findByStoreIdAndIsDeletedFalse(storeId))
                .willReturn(List.of(mockStoreImage1, mockStoreImage2));

        given(mockStoreImage1.getId()).willReturn(1L);
        given(mockStoreImage2.getId()).willReturn(2L);

        // Act
        final List<StoreImage> result = storeImageService.updateThumbnailImage(storeId, thumbnailId);

        // Assert
        assertThat(result).hasSize(2);

        then(mockStoreImage2).should(times(1))
                .changeThumbnail(true);
        then(mockStoreImage1).should(times(1))
                .changeThumbnail(false);
    }

    @DisplayName("Store 아이디에 해당하는 이미지들을 삭제한다.")
    @Test
    void deleteStoreImage() {
        // Arrange
        final long storeId = 1L;
        final String uploadUrl1 = "http://127.0.0.1:4566/pinup/store/image1.png";
        final String uploadUrl2 = "http://127.0.0.1:4566/pinup/store/image2.png";
        final String filename1 = "image1.png";
        final String filename2 = "image2.png";

        final StoreImage storeImage1 = getStoreImage(uploadUrl1, true);
        final StoreImage storeImage2 = getStoreImage(uploadUrl2, false);
        final List<StoreImage> storeImages = List.of(storeImage1, storeImage2);

        given(storeImageRepository.findByStoreIdAndIsDeletedFalse(storeId)).willReturn(storeImages);
        given(s3Service.extractFileName(uploadUrl1)).willReturn(filename1);
        given(s3Service.extractFileName(uploadUrl2)).willReturn(filename2);

        // Act
        storeImageService.deleteStoreImage(storeId);

        // Assert
        then(s3Service).should(times(1))
                .deleteFromS3(filename1);
        then(s3Service).should(times(1))
                .deleteFromS3(filename2);
        then(storeImageRepository).should(times(1))
                .deleteAll(storeImages);
    }

    @DisplayName("존재하지 않는 Store 아이디로 스토어 이미지를 삭제하면 예외가 발생한다.")
    @Test
    void deleteStoreImageWithNonExistStoreId() {
        // Arrange
        final long storeId = Long.MAX_VALUE;

        given(storeImageRepository.findByStoreIdAndIsDeletedFalse(storeId)).willReturn(List.of());

        // Act Assert
        assertThatThrownBy(() -> storeImageService.deleteStoreImage(storeId))
                .isInstanceOf(StoreImageNotFoundException.class)
                .hasMessage("해당 스토어 ID에 이미지가 존재하지 않습니다.");
    }

    private StoreCategory getStoreCategory() {
        return StoreCategory.builder()
                .name("패션")
                .build();
    }

    private Location getLocation() {
        return Location.builder()
                .name("서울 송파구 올림픽로 300")
                .zonecode("05551")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 300")
                .longitude(127.104302)
                .latitude(37.513713)
                .build();
    }

    private Store getStore(final StoreCategory storeCategory, final Location location) {
        return Store.builder()
                .name("팝업스토어")
                .description("팝업스토어가 올해도 여러분을 찾아갑니다!")
                .category(storeCategory)
                .location(location)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 7))
                .storeStatus(StoreStatus.PENDING)
                .build();
    }

    private StoreImage getStoreImage(final String url, final boolean isThumbnail) {
        return StoreImage.builder()
                .imageUrl(url)
                .isThumbnail(isThumbnail)
                .store(getStore(
                        getStoreCategory(), getLocation())
                )
                .build();
    }
}