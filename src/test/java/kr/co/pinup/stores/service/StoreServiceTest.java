package kr.co.pinup.stores.service;

import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.service.LocationService;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.service.StoreCategoryService;
import kr.co.pinup.storeimages.StoreImage;
import kr.co.pinup.storeimages.service.StoreImageService;
import kr.co.pinup.storeoperatinghour.StoreOperatingHour;
import kr.co.pinup.storeoperatinghour.model.dto.StoreOperatingHourRequest;
import kr.co.pinup.storeoperatinghour.service.StoreOperatingHourService;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.model.dto.*;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static kr.co.pinup.stores.model.enums.StoreStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreCategoryService categoryService;

    @Mock
    private LocationService locationService;

    @Mock
    private StoreImageService imageService;

    @Mock
    private StoreOperatingHourService operatingHourService;

    @Mock
    private S3Service s3Service;

    private ThreadPoolTaskExecutor taskExecutor;

    @InjectMocks
    private StoreService storeService;

    @BeforeEach
    void setUp() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.setQueueCapacity(10);
        taskExecutor.setThreadNamePrefix("test-s3-upload-");
        taskExecutor.initialize();

        storeService = new StoreService(
                storeRepository,
                categoryService,
                locationService,
                imageService,
                operatingHourService,
                s3Service,
                taskExecutor
        );
    }

    @AfterEach
    void tearDown() {
        if (taskExecutor != null) {
            taskExecutor.shutdown();
        }
    }

    @DisplayName("팝업스토어 전체 조회")
    @Test
    void getStores() {
        // Arrange
        final Store store1 = createStore("Store1", "Store Description1", RESOLVED);
        final Store store2 = createStore("Store2", "Store Description2", RESOLVED);

        final List<Store> stores = List.of(store1, store2);

        given(storeRepository.findAllByIsDeletedFalse()).willReturn(stores);

        // Act
        final List<StoreResponse> result = storeService.getStores();

        // Assert
        assertThat(result).hasSize(2)
                .extracting("name", "description", "status")
                .containsExactlyInAnyOrder(
                        tuple(store1.getName(), store1.getDescription(), store1.getStoreStatus()),
                        tuple(store2.getName(), store2.getDescription(), store2.getStoreStatus())
                );

        then(storeRepository).should(times(1))
                .findAllByIsDeletedFalse();
    }

    @DisplayName("진행상태, 자치구 필터로 팝업스토어 리스트 전체 조회")
    @Test
    void findAllWithStoreStatusNotAllAndSigunguNotAll() {
        // Arrange
        final StoreStatus storeStatus = RESOLVED;
        final String sigungu = "송파구";

        final Store store1 = createStore("Store 1", "Store description 1", RESOLVED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);

        given(storeRepository.findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, storeStatus))
                .willReturn(List.of(store1, store3));

        // Act
        final List<StoreThumbnailResponse> result = storeService.findAll(storeStatus, sigungu);

        // Assert
        assertThat(result).hasSize(2);

        then(storeRepository).should(times(1))
                .findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, storeStatus);
        then(storeRepository).should(times(0))
                .findAllByStoreStatusAndIsDeletedFalse(storeStatus);
        then(storeRepository).should(times(0))
                .findAllByLocation_SigunguAndIsDeletedFalse(sigungu);
        then(storeRepository).should(times(0))
                .findAllByIsDeletedFalse();
    }

    @DisplayName("진행상태 필터로 팝업스토어 리스트 전체 조회")
    @Test
    void findAllWithStoreStatusNotAllAndSigunguAll() {
        // Arrange
        final StoreStatus storeStatus = RESOLVED;
        final String sigungu = "all";

        final Store store1 = createStore("Store 1", "Store description 1", RESOLVED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);

        given(storeRepository.findAllByStoreStatusAndIsDeletedFalse(storeStatus)).willReturn(List.of(store1, store3));

        // Act
        final List<StoreThumbnailResponse> result = storeService.findAll(storeStatus, sigungu);

        // Assert
        assertThat(result).hasSize(2);

        then(storeRepository).should(times(0))
                .findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, storeStatus);
        then(storeRepository).should(times(1))
                .findAllByStoreStatusAndIsDeletedFalse(storeStatus);
        then(storeRepository).should(times(0))
                .findAllByLocation_SigunguAndIsDeletedFalse(sigungu);
        then(storeRepository).should(times(0))
                .findAllByIsDeletedFalse();
    }

    @DisplayName("자치구 필터로 팝업스토어 리스트 전체 조회")
    @Test
    void findAllWithStoreStatusAllAndSigunguNotAll() {
        // Arrange
        final StoreStatus storeStatus = null;
        final String sigungu = "송파구";

        final Store store1 = createStore("Store 1", "Store description 1", RESOLVED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);

        given(storeRepository.findAllByLocation_SigunguAndIsDeletedFalse(sigungu))
                .willReturn(List.of(store1, store2, store3));

        // Act
        final List<StoreThumbnailResponse> result = storeService.findAll(storeStatus, sigungu);

        // Assert
        assertThat(result).hasSize(3);

        then(storeRepository).should(times(0))
                .findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, storeStatus);
        then(storeRepository).should(times(0))
                .findAllByStoreStatusAndIsDeletedFalse(storeStatus);
        then(storeRepository).should(times(1))
                .findAllByLocation_SigunguAndIsDeletedFalse(sigungu);
        then(storeRepository).should(times(0))
                .findAllByIsDeletedFalse();
    }

    @DisplayName("필터 없이 팝업스토어 리스트 전체 조회")
    @Test
    void findAllWithStoreStatusAllAndSigunguAll() {
        // Arrange
        final StoreStatus storeStatus = null;
        final String sigungu = "all";

        final Store store1 = createStore("Store 1", "Store description 1", RESOLVED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);

        given(storeRepository.findAllByIsDeletedFalse()).willReturn(List.of(store1, store2, store3));

        // Act
        final List<StoreThumbnailResponse> result = storeService.findAll(storeStatus, sigungu);

        // Assert
        assertThat(result).hasSize(3);

        then(storeRepository).should(times(0))
                .findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, storeStatus);
        then(storeRepository).should(times(0))
                .findAllByStoreStatusAndIsDeletedFalse(storeStatus);
        then(storeRepository).should(times(0))
                .findAllByLocation_SigunguAndIsDeletedFalse(sigungu);
        then(storeRepository).should(times(1))
                .findAllByIsDeletedFalse();
    }

    @DisplayName("팝업스토어 상태를 정렬해서 limit 수만큼 조회한다")
    @Test
    void getStoresThumbnailWithLimit() {
        // Arrange
        final Store store1 = createStore("Store 1", "Store description 1", RESOLVED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);
        final Store store4 = createStore("Store 4", "Store description 4", RESOLVED);

        store1.addImages(List.of(
                createStoreImage("http://127.0.0.1:4566/pinup/store/image1.png")));
        store2.addImages(List.of(
                createStoreImage("http://127.0.0.1:4566/pinup/store/image2.png")));
        store3.addImages(List.of(
                createStoreImage("http://127.0.0.1:4566/pinup/store/image3.png")));
        store4.addImages(List.of(
                createStoreImage("http://127.0.0.1:4566/pinup/store/image4.png")));

        given(storeRepository.findAllByIsDeletedFalse()).willReturn(List.of(store1, store2, store3, store4));

        // Act
        final List<StoreThumbnailResponse> result = storeService.getStoresThumbnailWithLimit(3);

        // Assert
        assertThat(result).hasSize(3)
                .extracting("name", "status", "thumbnailImage")
                .containsExactly(
                        tuple(store1.getName(), store1.getStoreStatus(), store1.getStoreImages().get(0).getImageUrl()),
                        tuple(store3.getName(), store3.getStoreStatus(), store3.getStoreImages().get(0).getImageUrl()),
                        tuple(store4.getName(), store4.getStoreStatus(), store4.getStoreImages().get(0).getImageUrl())
                );

        then(storeRepository).should(times(1))
                .findAllByIsDeletedFalse();
    }

    @DisplayName("팝업스토어 상태를 우선순위로 정렬해서 조회한다")
    @Test
    void getStoresSortedByStatusPriority() {
        // Arrange
        final Store store1 = createStore("Store 1", "Store description 1", DISMISSED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);

        given(storeRepository.findAllByIsDeletedFalse()).willReturn(List.of(store1, store2, store3));

        // Act
        final List<StoreThumbnailResponse> result = storeService.getStoresSortedByStatusPriority();

        // Assert
        assertThat(result).hasSize(3)
                .extracting("name", "status")
                .containsExactly(
                        tuple(store3.getName(), store3.getStoreStatus()),
                        tuple(store2.getName(), store2.getStoreStatus()),
                        tuple(store1.getName(), store1.getStoreStatus())
                );

        then(storeRepository).should(times(1))
                .findAllByIsDeletedFalse();
    }

    @DisplayName("스토어 상태로 팝업스토어를 조회한다")
    @Test
    void getStoresByStatus() {
        // Arrange
        final StoreStatus storeStatus = PENDING;

        final Store store1 = createStore("Store 1", "Store description 1", PENDING);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);

        given(storeRepository.findAllByStoreStatusAndIsDeletedFalse(storeStatus))
                .willReturn(List.of(store1, store2));

        // Act
        final List<StoreThumbnailResponse> result = storeService.getStoresByStatus(storeStatus);

        // Assert
        assertThat(result).hasSize(2)
                .extracting("name", "status")
                .containsExactly(
                        tuple(store1.getName(), store1.getStoreStatus()),
                        tuple(store2.getName(), store2.getStoreStatus())
                );

        then(storeRepository).should(times(1))
                .findAllByStoreStatusAndIsDeletedFalse(storeStatus);
    }

    @DisplayName("스토어 ID로 해당 팝업스토어를 조회한다")
    @Test
    void getStoreById() {
        // Arrange
        final long storeId = 1L;

        final Store store = createStore("store 1", "description 1", PENDING);

        given(storeRepository.findById(storeId)).willReturn(Optional.ofNullable(store));

        // Act
        final StoreResponse result = storeService.getStoreById(storeId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(store.getName());
        assertThat(result.description()).isEqualTo(store.getDescription());
        assertThat(result.status()).isEqualTo(store.getStoreStatus());

        then(storeRepository).should(times(1))
                .findById(storeId);
    }

    @DisplayName("존재하지 않는 스토어 ID로 해당 팝업스토어를 조회하면 에외가 발생한다")
    @Test
    void getStoreByIdWithNonExistId() {
        // Arrange
        final long storeId = Long.MAX_VALUE;

        given(storeRepository.findById(storeId)).willThrow(new StoreNotFoundException());

        // Act Assert
        assertThatThrownBy(() -> storeService.getStoreById(storeId))
                .isInstanceOf(StoreNotFoundException.class)
                .hasMessage("해당 스토어가 존재하지 않습니다.");

        then(storeRepository).should(times(1))
                .findById(storeId);
    }

    @DisplayName("진행상태, 자치구 필터로 팝업스토어를 조회한다")
    @Test
    void getStoresByStatusAndLocationBySigungu() {
        // Arrange
        final StoreStatus storeStatus = PENDING;
        final String sigungu = "송파구";

        final Store store1 = createStore("Store 1", "Store description 1", RESOLVED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);

        given(storeRepository.findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, storeStatus))
                .willReturn(List.of(store2));

        // Act
        final List<StoreThumbnailResponse> result = storeService.getStoresByStatusAndLocationBySigungu(storeStatus, sigungu);

        // Assert
        assertThat(result).hasSize(1);

        then(storeRepository).should(times(1))
                .findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(sigungu, storeStatus);
    }

    @DisplayName("자치구 필터로 팝업스토어를 조회한다")
    @Test
    void getStoresByLocationBySigungu() {
        // Arrange
        final String sigungu = "송파구";

        final Store store1 = createStore("Store 1", "Store description 1", RESOLVED);
        final Store store2 = createStore("Store 2", "Store description 2", PENDING);
        final Store store3 = createStore("Store 3", "Store description 3", RESOLVED);

        given(storeRepository.findAllByLocation_SigunguAndIsDeletedFalse(sigungu))
                .willReturn(List.of(store1, store2, store3));

        // Act
        final List<StoreThumbnailResponse> result = storeService.getStoresByLocationBySigungu(sigungu);

        // Assert
        assertThat(result).hasSize(3);

        then(storeRepository).should(times(1))
                .findAllByLocation_SigunguAndIsDeletedFalse(sigungu);
    }

    @DisplayName("S3 업로드 및 팝업스토어 저장을 위한 선행 작업")
    @Test
    void createStore() {
        // Arrange
        final StoreRequest request = createStoreRequest();
        final LocalDate today = LocalDate.now();

        final StoreCategory mockCategory = mock(StoreCategory.class);
        final Location mockLocation = mock(Location.class);

        given(s3Service.uploadFile(any(MultipartFile.class), anyString()))
                .willReturn("http://s3.amazonaws.com/bucket/image.jpg");
        given(categoryService.findCategoryById(1L)).willReturn(mockCategory);
        given(locationService.getLocation(1L)).willReturn(mockLocation);
        given(storeRepository.save(any(Store.class)))
                .willAnswer(invocation -> {
                    Store store = invocation.getArgument(0);
                    ReflectionTestUtils.setField(store, "id", 1L);
                    ReflectionTestUtils.setField(store, "createdAt", LocalDateTime.now());
                    return store;
                });

        // Act
        final StoreCreateResponse result = storeService.createStore(
                request,
                List.of(mock(MultipartFile.class)),
                today
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();
        assertThat(result.createdAt()).isNotNull();

        then(s3Service).should(times(1))
                .uploadFile(any(MultipartFile.class), anyString());
        then(categoryService).should(times(1))
                .findCategoryById(1L);
        then(locationService).should(times(1))
                .getLocation(1L);
        then(operatingHourService).should(times(1))
                .createOperatingHours(any(), any());
        then(imageService).should(times(1))
                .createUploadImages(any(), any(), eq(0L));
        then(storeRepository).should(times(1))
                .save(any(Store.class));
    }

    @DisplayName("하나의 트랜잭션에서 팝업스토어를 저장한다")
    @Test
    void createStoreTransactional() {
        // Arrange
        final StoreRequest request = createStoreRequest();
        final List<String> uploadUrls = List.of("http://127.0.0.1:4566/pinup/store/image.png");
        final Store store = Store.builder().build();

        final List<StoreOperatingHour> mockOperatingHours = List.of(mock(StoreOperatingHour.class));
        final List<StoreImage> mockStoreImages = List.of(mock(StoreImage.class));

        given(operatingHourService.createOperatingHours(eq(store), any()))
                .willReturn(mockOperatingHours);
        given(imageService.createUploadImages(store, uploadUrls, 0L))
                .willReturn(mockStoreImages);
        given(storeRepository.save(store)).willReturn(store);

        // Act
        storeService.createStoreTransactional(request, uploadUrls, store);

        // Assert
        assertThat(store.getOperatingHours()).containsExactlyElementsOf(mockOperatingHours);
        assertThat(store.getStoreImages()).containsExactlyElementsOf(mockStoreImages);

        then(operatingHourService).should(times(1))
                .createOperatingHours(eq(store), any());
        then(imageService).should(times(1))
                .createUploadImages(store, uploadUrls, 0L);
        then(storeRepository).should(times(1))
                .save(store);
    }

    @DisplayName("팝업스토어 ID로 팝업스토어 정보를 수정한다")
    @Test
    void updateStore() {
        // Arrange
        final long storeId = 1L;
        final StoreUpdateRequest request = createStoreUpdateRequest();
        List<MultipartFile> images = List.of();

        final Store mockStore = mock(Store.class);
        final StoreCategory mockCategory = mock(StoreCategory.class);
        final Location mockLocation = mock(Location.class);
        final List<StoreOperatingHour> mockOperatingHours = List.of(mock(StoreOperatingHour.class));
        final List<StoreImage> mockImages = List.of(mock(StoreImage.class));

        given(mockStore.getCategory()).willReturn(mockCategory);
        given(mockStore.getLocation()).willReturn(mockLocation);

        given(storeRepository.findById(storeId)).willReturn(Optional.of(mockStore));
        given(categoryService.findCategoryById(request.categoryId())).willReturn(mockCategory);
        given(locationService.getLocation(request.locationId())).willReturn(mockLocation);
        given(operatingHourService.createOperatingHours(mockStore, request.operatingHours()))
                .willReturn(mockOperatingHours);
        given(imageService.updateThumbnailImage(storeId, request.thumbnailId()))
                .willReturn(mockImages);

        // Act
        final StoreResponse result = storeService.updateStore(storeId, request, images);

        // Assert
        assertThat(result).isNotNull();

        then(mockStore).should(times(1))
                .update(request, mockCategory, mockLocation);
        then(mockStore).should(times(1))
                .operatingHoursClear();
        then(imageService).should(times(1))
                .removeStoreImage(storeId, request.deletedImageIds());
        then(imageService).should(times(1))
                .updateThumbnailImage(storeId, request.thumbnailId());
        then(imageService).should(never())
                .updateUploadImages(any(), any(), anyLong(), anyLong());
        then(mockStore).should(times(1))
                .addImages(mockImages);
    }

    @DisplayName("존재하지 않는 팝업스토어 ID로 팝업스토어 수정 시 예외가 발생한다")
    @Test
    void updateStoreWithNonExistId() {
        // Arrange
        final long storeId = Long.MAX_VALUE;
        final StoreUpdateRequest request = createStoreUpdateRequest();
        List<MultipartFile> images = List.of();

        given(storeRepository.findById(storeId)).willThrow(new StoreNotFoundException());

        // Act Assert
        assertThatThrownBy(() -> storeService.updateStore(storeId, request, images))
                .isInstanceOf(StoreNotFoundException.class)
                .hasMessage("해당 스토어가 존재하지 않습니다.");

        then(storeRepository).should(times(1))
                .findById(storeId);
    }

    @DisplayName("팝업스토어 삭제 시 isDeleted를 true로 변경한다")
    @Test
    void deleteStore() {
        // Arrange
        final long storeId = 1L;

        final Store mockStore = mock(Store.class);

        given(storeRepository.findById(storeId)).willReturn(Optional.ofNullable(mockStore));

        // Act
        storeService.deleteStore(storeId);

        // Assert
        then(storeRepository).should(times(1))
                .findById(storeId);
        then(mockStore).should(times(1))
                .deleteStore(true);
    }

    @DisplayName("존재하지 않는 팝업스토어 ID로 팝업스토어 삭제 시 예외가 발생한다")
    @Test
    void deleteStoreWithNonExistId() {
        // Arrange
        final long storeId = Long.MAX_VALUE;

        given(storeRepository.findById(storeId)).willThrow(new StoreNotFoundException());

        // Act Assert
        assertThatThrownBy(() -> storeService.deleteStore(storeId))
                .isInstanceOf(StoreNotFoundException.class)
                .hasMessage("해당 스토어가 존재하지 않습니다.");

        then(storeRepository).should(times(1))
                .findById(storeId);
    }

    private StoreCategory createCategory() {
        return StoreCategory.builder()
                .name("뷰티")
                .build();
    }

    private Location createLocation() {
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

    private StoreImage createStoreImage(final String url) {
        return StoreImage.builder()
                .imageUrl(url)
                .isThumbnail(true)
                .build();
    }

    private StoreOperatingHourRequest createOperatingHourRequest() {
        return StoreOperatingHourRequest.builder()
                .days("월~금")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(20, 0))
                .build();
    }

    private Store createStore(final String name, final String description, final StoreStatus status) {
        return Store.builder()
                .name(name)
                .description(description)
                .storeStatus(status)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .snsUrl("https://instgram.com/test")
                .category(createCategory())
                .location(createLocation())
                .build();
    }

    private StoreRequest createStoreRequest() {
        return StoreRequest.builder()
                .name("Store")
                .description("Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .snsUrl("https://instgram.com/test")
                .thumbnailIndex(0L)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(createOperatingHourRequest()))
                .build();
    }

    private StoreUpdateRequest createStoreUpdateRequest() {
        return StoreUpdateRequest.builder()
                .name("Store")
                .description("Description")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .snsUrl("https://instgram.com/test")
                .thumbnailId(1L)
                .thumbnailIndex(null)
                .categoryId(1L)
                .locationId(1L)
                .operatingHours(List.of(createOperatingHourRequest()))
                .deletedImageIds(List.of())
                .build();
    }
}